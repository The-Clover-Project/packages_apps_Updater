/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.clover.updater.data.source.network

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import com.clover.updater.R
import com.clover.updater.deviceinfo.DeviceInfoUtils
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdatesNetworkDataSource(private val context: Context) {
    private val serverUrl: String
        get() {
            val base = context.getString(R.string.updater_server_url)
            require(base.startsWith("https://")) {
                "Update server URL must use HTTPS: $base"
            }
            if (base.contains("{flavour}") && DeviceInfoUtils.flavour.isEmpty()) {
                throw IOException("Unknown build flavour")
            }
            return base
                .replace("{device}", DeviceInfoUtils.device)
                .replace("{type}", DeviceInfoUtils.releaseType.lowercase())
                .replace("{flavour}", DeviceInfoUtils.flavour)
        }

    private val compatibilityUrl: String
        get() {
            val url = context.getString(R.string.updater_compatibility_url)
            require(url.startsWith("https://")) {
                "Compatibility URL must use HTTPS: $url"
            }
            return url
        }

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        return client.newCall(request).execute().use { response ->
            if (response.code == 404) {
                return null
            }
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP status: ${response.code}")
            }

            response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    fun fetchUpdates(): List<NetworkUpdate> =
        get(serverUrl)?.let { Json.decodeFromString<NetworkUpdatesResponse>(it).response }
            .orEmpty()

    fun fetchCompatibility(): String? = get(compatibilityUrl)
}
