/*
 * SPDX-FileCopyrightText: The Clover Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.clover.updater.download

import android.util.Log
import java.io.File
import java.io.IOException

object DownloadState {
    private const val TAG = "DownloadState"
    private const val SUFFIX = ".parts"
    private const val HEADER = "v1"

    data class Segment(val start: Long, val end: Long, val done: Long) {
        val length: Long
            get() = end - start + 1
    }

    data class State(val total: Long, val segments: List<Segment>) {
        val downloaded: Long
            get() = segments.sumOf { it.done }
    }

    @JvmStatic
    fun stateFile(file: File) = File(file.path + SUFFIX)

    @JvmStatic
    fun isIncomplete(file: File) = stateFile(file).exists()

    @JvmStatic
    fun downloadedBytes(file: File?): Long {
        if (file == null || !file.exists()) return 0
        if (!isIncomplete(file)) return file.length()
        return read(file)?.downloaded ?: 0
    }

    @JvmStatic
    fun delete(file: File?) {
        if (file != null) {
            stateFile(file).delete()
        }
    }

    fun split(total: Long, count: Int): State {
        val size = total / count
        val segments = (0 until count).map { i ->
            val start = i * size
            val end = if (i == count - 1) total - 1 else start + size - 1
            Segment(start, end, 0)
        }
        return State(total, segments)
    }

    fun read(file: File): State? = if (!isIncomplete(file)) null else try {
        val lines = stateFile(file).readLines().filter { it.isNotBlank() }
        val header = lines.first().split(" ")
        require(header.size == 2 && header[0] == HEADER)
        val total = header[1].toLong()
        val segments = lines.drop(1).map { line ->
            val parts = line.split(" ").map { it.toLong() }
            require(parts.size == 3)
            Segment(parts[0], parts[1], parts[2])
        }
        require(segments.isNotEmpty() && segments.first().start == 0L && segments.last().end == total - 1)
        segments.zipWithNext().forEach { (a, b) -> require(a.end + 1 == b.start) }
        segments.forEach { require(it.done in 0..it.length) }
        State(total, segments)
    } catch (e: Exception) {
        Log.e(TAG, "Invalid download state for ${file.name}", e)
        null
    }

    fun write(file: File, state: State) {
        val target = stateFile(file)
        val tmp = File(target.path + ".tmp")
        tmp.writeText(buildString {
            append("$HEADER ${state.total}\n")
            state.segments.forEach { append("${it.start} ${it.end} ${it.done}\n") }
        })
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("Could not save download state")
        }
    }
}
