/*
 * SPDX-FileCopyrightText: The Clover Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.clover.updater.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import com.clover.updater.data.source.network.NetworkCompatibility
import com.clover.updater.deviceinfo.DeviceInfoUtils
import com.clover.updater.util.CloverVersion
import java.io.File

object OtaCompatibility {
    private const val TAG = "OtaCompatibility"
    private const val CACHE_FILE = "ota_compatibility.json"

    @Volatile
    private var cleanRequired: List<CloverVersion> = emptyList()

    @JvmStatic
    fun load(context: Context) {
        runCatching {
            val file = File(context.filesDir, CACHE_FILE)
            if (file.exists()) {
                cleanRequired = parse(file.readText())
            }
        }.onFailure { Log.e(TAG, "Could not read cached compatibility data", it) }
    }

    fun update(context: Context, json: String) {
        cleanRequired = parse(json)
        val file = File(context.filesDir, CACHE_FILE)
        val tmp = File(context.filesDir, "$CACHE_FILE.tmp")
        tmp.writeText(json)
        if (!tmp.renameTo(file)) {
            tmp.delete()
        }
    }

    private fun parse(json: String): List<CloverVersion> {
        val data = Json.decodeFromString<NetworkCompatibility>(json)
        val device = data.devices
            .filterKeys { it.equals(DeviceInfoUtils.device, ignoreCase = true) }
            .values.flatMap { it.cleanRequired }
        return (data.cleanRequired + device).mapNotNull { CloverVersion.parse(it) }.distinct()
    }

    @JvmStatic
    fun requiresCleanFlash(update: Update): Boolean {
        val current = CloverVersion.parse(DeviceInfoUtils.buildVersion) ?: return false
        val target = CloverVersion.parse(update.version) ?: return false
        return cleanRequired.any { it > current && it <= target }
    }
}
