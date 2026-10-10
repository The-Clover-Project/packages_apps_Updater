/*
 * SPDX-FileCopyrightText: The Clover Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.clover.updater.data.source.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkCompatibility(
    @SerialName("ver_clean_required") val cleanRequired: List<String> = emptyList(),
    @SerialName("ver_dirty_allowed") val dirtyAllowed: List<String> = emptyList(),
    @SerialName("devices") val devices: Map<String, NetworkDeviceCompatibility> = emptyMap(),
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkDeviceCompatibility(
    @SerialName("ver_clean_required") val cleanRequired: List<String> = emptyList(),
)
