/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.clover.updater.data.source.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import com.clover.updater.data.Update
import com.clover.updater.deviceinfo.DeviceInfoUtils
import com.clover.updater.misc.Constants

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkUpdatesResponse(
    @SerialName("response") val response: List<NetworkUpdate>,
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkUpdate(
    @SerialName("datetime") val datetime: Long,
    @SerialName("filename") val filename: String,
    @SerialName("id") val id: String,
    @SerialName("os_patch_level") val osPatchLevel: String? = null,
    @SerialName("os_sdk_level") val osSdkLevel: Int? = null,
    @SerialName("ota_property_files") val otaPropertyFiles: String? = null,
    @SerialName("romtype") val romtype: String,
    @SerialName("size") val size: Long,
    @SerialName("url") val url: String,
    @SerialName("version") val version: String,
)

private data class PackageFileRange(
    val offset: Long,
    val size: Long,
)

private fun String.parsePackageFileRanges() =
    split(",").mapNotNull { token ->
        val parts = token.trim().split(":")
        if (parts.size < 3) return@mapNotNull null
        val offset = parts[1].trim().toLongOrNull() ?: return@mapNotNull null
        val size = parts[2].trim().toLongOrNull() ?: return@mapNotNull null
        parts[0].trim() to PackageFileRange(offset, size)
    }.toMap()

fun NetworkUpdate.toUpdate(): Update {
    val packageFileRanges = otaPropertyFiles?.parsePackageFileRanges().orEmpty()
    val payloadMetadataRange = packageFileRanges[Constants.AB_PAYLOAD_METADATA_PATH]
    val payloadRange = packageFileRanges[Constants.AB_PAYLOAD_BIN_PATH]
    val payloadPropertiesRange = packageFileRanges[Constants.AB_PAYLOAD_PROPERTIES_PATH]

    return Update(
        downloadId = id,
        name = filename,
        timestamp = datetime,
        type = romtype,
        fileSize = size,
        downloadUrl = url,
        version = version,
        osPatchLevel = osPatchLevel,
        osSdkLevel = osSdkLevel ?: DeviceInfoUtils.sdkLevel,
        payloadMetadataOffset = payloadMetadataRange?.offset,
        payloadMetadataSize = payloadMetadataRange?.size,
        payloadOffset = payloadRange?.offset,
        payloadSize = payloadRange?.size,
        payloadPropertiesOffset = payloadPropertiesRange?.offset,
        payloadPropertiesSize = payloadPropertiesRange?.size,
        isAvailableOnline = true,
    )
}
