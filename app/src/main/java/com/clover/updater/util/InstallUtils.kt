/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.clover.updater.util

import com.clover.updater.data.Update
import com.clover.updater.deviceinfo.DeviceInfoUtils
import com.clover.updater.misc.Utils
import java.io.File

object InstallUtils {
    @JvmStatic
    fun isScratchMounted() = runCatching {
        File("/proc/mounts").useLines { lines ->
            lines.any { it.split(" ")[1] == "/mnt/scratch" }
        }
    }.getOrDefault(false)

    enum class BlockedReason {
        NONE, DOWNGRADE, VERSION_UNSUPPORTED
    }

    @JvmStatic
    fun getBlockedReason(update: Update) = when {
        !DeviceInfoUtils.isDowngradingAllowed
                && update.timestamp <= DeviceInfoUtils.buildDateTimestamp
            -> BlockedReason.DOWNGRADE

        !Utils.compareVersions(
            update.version, DeviceInfoUtils.buildVersion, DeviceInfoUtils.isMajorUpdateAllowed
        ) -> BlockedReason.VERSION_UNSUPPORTED

        else -> BlockedReason.NONE
    }

    @JvmStatic
    fun canInstall(update: Update) = getBlockedReason(update) == BlockedReason.NONE

    @JvmStatic
    fun canStreamUpdate(update: Update, streamUpdatesEnabled: Boolean) =
        DeviceInfoUtils.isABDevice &&
                streamUpdatesEnabled &&
                update.isAvailableOnline &&
                update.hasPayloadFileRanges() &&
                !update.hasFullyDownloadedPackage() &&
                !update.hasVerifiedPackage()
}
