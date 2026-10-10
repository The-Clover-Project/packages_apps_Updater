/*
 * SPDX-FileCopyrightText: The Clover Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.clover.updater.util

data class CloverVersion(
    val major: Int,
    val minor: Int,
) : Comparable<CloverVersion> {

    override fun compareTo(other: CloverVersion) =
        compareValuesBy(this, other, CloverVersion::major, CloverVersion::minor)

    companion object {
        private val PATTERN = Regex("^(\\d+)(?:\\.(\\d+))?$")

        @JvmStatic
        fun parse(value: String?): CloverVersion? {
            val match = PATTERN.matchEntire(value?.trim().orEmpty()) ?: return null
            return CloverVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groups[2]?.value?.toIntOrNull() ?: 0,
            )
        }
    }
}
