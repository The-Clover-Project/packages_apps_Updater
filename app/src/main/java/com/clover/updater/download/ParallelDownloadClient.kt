/*
 * SPDX-FileCopyrightText: The Clover Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.clover.updater.download

import android.os.SystemClock
import android.util.Log
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class ParallelDownloadClient internal constructor(
    private val url: String,
    private val destination: File,
    private val progressListener: DownloadClient.ProgressListener?,
    private val callback: DownloadClient.DownloadCallback,
    private val fallback: DownloadClient,
) : DownloadClient {

    private var coordinator: Coordinator? = null
    private var delegate: DownloadClient? = null

    @Synchronized
    override fun start() {
        if (coordinator != null || delegate != null) {
            Log.e(TAG, "Already downloading")
            return
        }
        coordinator = Coordinator(resume = false).also { it.start() }
    }

    @Synchronized
    override fun resume() {
        if (coordinator != null || delegate != null) {
            Log.e(TAG, "Already downloading")
            return
        }
        if (!destination.exists()) {
            callback.onFailure(false)
            return
        }
        if (!DownloadState.isIncomplete(destination)) {
            delegate = fallback
            fallback.resume()
            return
        }
        coordinator = Coordinator(resume = true).also { it.start() }
    }

    @Synchronized
    override fun cancel() {
        val current = coordinator
        val currentDelegate = delegate
        when {
            current != null -> current.abort()
            currentDelegate != null -> currentDelegate.cancel()
            else -> Log.e(TAG, "Not downloading")
        }
        coordinator = null
        delegate = null
    }

    @Synchronized
    private fun handOver(from: Coordinator): Boolean {
        if (coordinator !== from) return false
        coordinator = null
        delegate = fallback
        fallback.start()
        return true
    }

    private inner class Coordinator(private val resume: Boolean) : Thread("ParallelDownload") {

        @Volatile
        private var aborted = false

        @Volatile
        private var probeCall: Call? = null

        private val workers = mutableListOf<SegmentWorker>()
        private val downloaded = AtomicLong(0)

        private var speed = -1L
        private var lastSampleMillis = 0L
        private var lastSampleBytes = 0L

        fun abort() {
            aborted = true
            probeCall?.cancel()
            synchronized(workers) {
                workers.forEach { it.abort() }
            }
        }

        override fun run() {
            val key = destination.absolutePath
            try {
                active.put(key, this)?.join()
                download()
            } catch (e: InterruptedException) {
                callback.onFailure(true)
            } finally {
                active.remove(key, this)
            }
        }

        private fun download() {
            val total = try {
                probe()
            } catch (e: IOException) {
                Log.e(TAG, "Could not reach the download server", e)
                callback.onFailure(aborted)
                return
            }
            if (aborted) {
                callback.onFailure(true)
                return
            }

            if (total == null || total < MIN_PARALLEL_SIZE) {
                if (resume) {
                    Log.w(TAG, "The server cannot continue a split download, starting again")
                    DownloadState.delete(destination)
                }
                if (!handOver(this)) {
                    callback.onFailure(true)
                } else {
                    Log.d(TAG, "Using a single connection")
                }
                return
            }

            if (resume && !DownloadState.isIncomplete(destination) &&
                destination.length() == total
            ) {
                Log.d(TAG, "Download already complete")
                callback.onResponse { name ->
                    if (name.equals("Content-Length", ignoreCase = true)) total.toString() else null
                }
                progressListener?.update(total, total, -1, -1)
                callback.onSuccess()
                return
            }

            val state = try {
                val saved = if (resume) DownloadState.read(destination) else null
                if (saved != null && saved.total == total) {
                    saved
                } else {
                    if (resume) {
                        Log.d(TAG, "Saved download state does not match, starting again")
                    }
                    RandomAccessFile(destination, "rw").use { it.setLength(0) }
                    DownloadState.split(total, THREADS).also { DownloadState.write(destination, it) }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Could not prepare the download file", e)
                callback.onFailure(false)
                return
            }

            callback.onResponse { name ->
                if (name.equals("Content-Length", ignoreCase = true)) total.toString() else null
            }

            try {
                RandomAccessFile(destination, "rw").use { file ->
                    runWorkers(file.channel, state)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error downloading file", e)
                callback.onFailure(aborted)
            }
        }

        private fun runWorkers(channel: FileChannel, state: DownloadState.State) {
            val total = state.total
            downloaded.set(state.downloaded)
            lastSampleBytes = downloaded.get()
            lastSampleMillis = SystemClock.elapsedRealtime()
            synchronized(workers) {
                state.segments.forEach { workers.add(SegmentWorker(it, channel)) }
                if (aborted) {
                    workers.forEach { it.abort() }
                }
            }
            workers.forEach { it.start() }

            var lastSave = SystemClock.elapsedRealtime()
            while (workers.any { it.isAlive }) {
                workers.firstOrNull { it.isAlive }?.join(REPORT_INTERVAL_MS)
                if (workers.any { it.failed }) {
                    workers.forEach { it.abort() }
                }
                report(total)
                val now = SystemClock.elapsedRealtime()
                if (now - lastSave >= SAVE_INTERVAL_MS) {
                    save(channel, total)
                    lastSave = now
                }
            }

            if (aborted || !workers.all { it.isComplete }) {
                save(channel, total)
                callback.onFailure(aborted)
                return
            }

            channel.force(true)
            DownloadState.delete(destination)
            report(total)
            callback.onSuccess()
        }

        private fun save(channel: FileChannel, total: Long) {
            if (!destination.exists()) return
            try {
                channel.force(false)
                DownloadState.write(
                    destination,
                    DownloadState.State(total, workers.map { it.snapshot() }),
                )
            } catch (e: IOException) {
                Log.e(TAG, "Could not save download state", e)
            }
        }

        private fun report(total: Long) {
            val bytes = downloaded.get()
            val now = SystemClock.elapsedRealtime()
            val delta = now - lastSampleMillis
            if (delta >= SPEED_SAMPLE_MS) {
                val current = (bytes - lastSampleBytes) * 1000 / delta
                speed = if (speed < 0) current else (speed * 3 + current) / 4
                lastSampleMillis = now
                lastSampleBytes = bytes
            }
            val eta = if (speed > 0) maxOf((total - bytes) / speed, 1) else -1
            progressListener?.update(bytes, total, speed, eta)
        }

        @Throws(IOException::class)
        private fun probe(): Long? {
            val call = client.newCall(request("bytes=0-0"))
            probeCall = call
            if (aborted) return null
            call.execute().use { response ->
                if (response.code == HttpURLConnection.HTTP_PARTIAL) {
                    return response.header("Content-Range")
                        ?.substringAfter('/', "")
                        ?.toLongOrNull()
                }
                if (response.isSuccessful) return null
                throw IOException("Server replied with ${response.code}")
            }
        }

        private inner class SegmentWorker(
            private val segment: DownloadState.Segment,
            private val channel: FileChannel,
        ) : Thread("ParallelDownload-${segment.start}") {

            private val done = AtomicLong(segment.done)
            private val stopSignal = CountDownLatch(1)

            @Volatile
            private var call: Call? = null

            @Volatile
            private var stopped = false

            @Volatile
            var failed = false
                private set

            val isComplete: Boolean
                get() = done.get() >= segment.length

            fun snapshot() = segment.copy(done = done.get())

            fun abort() {
                stopped = true
                call?.cancel()
                stopSignal.countDown()
            }

            override fun run() {
                var failures = 0
                while (!stopped && !isComplete) {
                    val from = segment.start + done.get()
                    val current = client.newCall(request("bytes=$from-${segment.end}"))
                    call = current
                    if (stopped) return
                    try {
                        current.execute().use { response ->
                            if (response.code != HttpURLConnection.HTTP_PARTIAL) {
                                throw IOException("Server replied with ${response.code}")
                            }
                            val contentRange = response.header("Content-Range").orEmpty()
                            if (!contentRange.startsWith("bytes $from-")) {
                                throw IOException("Unexpected Content-Range: $contentRange")
                            }
                            val input = response.body?.byteStream()
                                ?: throw IOException("Empty response body")
                            val buffer = ByteArray(CHUNK_SIZE)
                            var position = from
                            while (!stopped && position <= segment.end) {
                                val wanted = minOf(buffer.size.toLong(), segment.end - position + 1)
                                val count = input.read(buffer, 0, wanted.toInt())
                                if (count < 0) {
                                    throw IOException("Connection closed at $position")
                                }
                                val data = ByteBuffer.wrap(buffer, 0, count)
                                var offset = position
                                while (data.hasRemaining()) {
                                    offset += channel.write(data, offset)
                                }
                                position += count
                                done.addAndGet(count.toLong())
                                downloaded.addAndGet(count.toLong())
                                failures = 0
                            }
                        }
                    } catch (e: IOException) {
                        if (stopped) return
                        failures++
                        Log.w(TAG, "Segment ${segment.start} failed ($failures/$MAX_FAILURES)", e)
                        if (failures >= MAX_FAILURES) {
                            failed = true
                            return
                        }
                        if (stopSignal.await(RETRY_DELAY_MS * failures, TimeUnit.MILLISECONDS)) {
                            return
                        }
                    }
                }
            }
        }
    }

    private fun request(range: String) = Request.Builder()
        .url(url)
        .header("Range", range)
        .header("Accept-Encoding", "identity")
        .build()

    companion object {
        private const val TAG = "ParallelDownloadClient"

        private const val THREADS = 3
        private const val MIN_PARALLEL_SIZE = 16L * 1024 * 1024
        private const val CHUNK_SIZE = 64 * 1024
        private const val MAX_FAILURES = 5
        private const val RETRY_DELAY_MS = 2000L
        private const val REPORT_INTERVAL_MS = 500L
        private const val SPEED_SAMPLE_MS = 500L
        private const val SAVE_INTERVAL_MS = 2000L

        private val active = ConcurrentHashMap<String, Thread>()

        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followSslRedirects(false)
            .build()
    }
}
