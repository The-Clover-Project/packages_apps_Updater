/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.clover.updater.updates

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsShape
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.ui.LinearProgressBar
import com.clover.updater.R
import com.clover.updater.updates.action.UpdateAction
import com.clover.updater.updates.action.UpdateActionType
import com.clover.updater.updates.action.UpdateActions
import com.clover.updater.updates.button.ActionBar
import com.clover.updater.updates.button.ActionBarButton
import com.clover.updater.updates.state.ProgressState
import com.clover.updater.updates.state.UpdateItemState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateItem(
    state: UpdateItemState,
    expanded: Boolean,
    onExpandToggle: (() -> Unit)?,
    onAction: (UpdateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsShape.CornerExtraSmall2)
            .clickable(
                enabled = onExpandToggle != null,
                onClick = { onExpandToggle?.invoke() }
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = SettingsShape.CornerExtraSmall2
    ) {
        Column {
            key(state.downloadId) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SettingsDimension.itemPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!expanded) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isLocal) Icons.Outlined.Archive else Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(SettingsSpace.medium1))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.buildDate,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val summary = buildString {
                            append(state.buildVersion)
                            if (state.status.isNotEmpty()) {
                                append(" • ")
                                append(state.status)
                            }
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Column(
                        modifier = Modifier.padding(horizontal = SettingsDimension.itemPaddingStart),
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                            LocalTextStyle provides MaterialTheme.typography.bodySmall,
                        ) {
                            when (val progress = state.progress) {
                                is ProgressState.Determinate -> {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                MaterialTheme.typography.headlineMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                                            ) {
                                                append(progress.percent.toInt().toString())
                                            }
                                            append("%")
                                        }
                                    )
                                    val secondaryText = buildString {
                                        if (progress.downloadedSize.isNotEmpty()) append(progress.downloadedSize)
                                        if (progress.downloadedSize.isNotEmpty() && progress.eta.isNotEmpty()) append(" • ")
                                        if (progress.eta.isNotEmpty()) append(progress.eta)
                                    }
                                    if (secondaryText.isNotEmpty()) {
                                        Text(
                                            text = secondaryText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(SettingsSpace.small1))
                                    LinearProgressBar(progress = progress.percent / 100f)
                                }

                                ProgressState.Indeterminate -> {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }

                                null -> {
                                    Text(text = stringResource(R.string.list_update_size, state.fileSize))
                                    if (state.androidUpdateInfo.isNotEmpty()) {
                                        Text(text = state.androidUpdateInfo)
                                    }
                                    if (state.securityUpdate.isNotEmpty()) {
                                        Text(
                                            text = stringResource(
                                                R.string.list_security_update,
                                                state.securityUpdate,
                                            )
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            top = SettingsSpace.small1,
                                            bottom = SettingsSpace.extraSmall4,
                                        )
                                    )
                                    Text(
                                        text = if (state.installNote == R.string.list_major_upgrade_recovery_install) {
                                            stringResource(
                                                state.installNote,
                                                stringResource(R.string.brand_name),
                                            )
                                        } else {
                                            stringResource(state.installNote)
                                        },
                                        style = MaterialTheme.typography.bodySmallEmphasized,
                                    )
                                }
                            }
                        }
                    }

                    ActionBar(
                        buttons = listOfNotNull(
                            state.actions.primary.toActionBarButton(
                                context,
                                onAction,
                                isPrimary = true
                            ),
                            state.actions.secondary?.toActionBarButton(context, onAction),
                        ),
                        menuContent = if (state.actions.overflow.isEmpty()) {
                            null
                        } else {
                            {
                                state.actions.overflow.forEach { action ->
                                    MenuItem(
                                        text = action.type.title(context),
                                        enabled = action.enabled,
                                    ) {
                                        onAction(action)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun UpdateAction.toActionBarButton(
    context: Context,
    onAction: (UpdateAction) -> Unit,
    isPrimary: Boolean = false,
): ActionBarButton = when (type) {
    UpdateActionType.PAUSE_DOWNLOAD,
    UpdateActionType.PAUSE_INSTALL -> ActionBarButton.Icon(
        imageVector = Icons.Outlined.Pause,
        contentDescription = type.title(context),
        enabled = enabled,
        onClick = { onAction(this) },
    )

    UpdateActionType.RESUME_DOWNLOAD,
    UpdateActionType.RESUME_INSTALL -> ActionBarButton.Icon(
        imageVector = Icons.Outlined.PlayArrow,
        contentDescription = type.title(context),
        enabled = enabled,
        onClick = { onAction(this) },
    )

    else -> if (isPrimary) {
        ActionBarButton.Tonal(
            text = type.title(context),
            enabled = enabled,
            onClick = { onAction(this) },
        )
    } else {
        ActionBarButton.Outlined(
            text = type.title(context),
            enabled = enabled,
            onClick = { onAction(this) },
        )
    }
}

@UiModePreviews
@Composable
private fun UpdateItemIdleCollapsedPreview() {
    com.clover.updater.theme.UpdaterTheme {
        UpdateItem(
            state = UpdateItemState(
                downloadId = "preview",
                buildDate = "Apr 27",
                buildVersion = "Clover 4.0",
                status = "",
                isLocal = false,
                fileSize = "1.1 GB",
                androidUpdateInfo = "Android 17",
                securityUpdate = "Jan 2026",
                installNote = R.string.list_full_install,
                progress = null,
                actions = UpdateActions(
                    primary = UpdateAction(
                        type = UpdateActionType.START_DOWNLOAD,
                    ),
                    overflow = listOf(
                        UpdateAction(
                            type = UpdateActionType.VIEW_DOWNLOADS,
                        ),
                    ),
                ),
            ),
            expanded = false,
            onExpandToggle = {},
            onAction = {},
        )
    }
}

@UiModePreviews
@Composable
private fun UpdateItemIdleExpandedPreview() {
    com.clover.updater.theme.UpdaterTheme {
        UpdateItem(
            state = UpdateItemState(
                downloadId = "preview",
                buildDate = "Apr 27",
                buildVersion = "Clover 4.0",
                status = "",
                isLocal = false,
                fileSize = "1.1 GB",
                androidUpdateInfo = "Major Android upgrade",
                securityUpdate = "Jan 2026",
                installNote = R.string.list_major_upgrade_recovery_install,
                progress = null,
                actions = UpdateActions(
                    primary = UpdateAction(
                        type = UpdateActionType.OPEN_GUIDE,
                    ),
                ),
            ),
            expanded = true,
            onExpandToggle = {},
            onAction = {},
        )
    }
}

@UiModePreviews
@Composable
private fun UpdateItemDownloadingPreview() {
    com.clover.updater.theme.UpdaterTheme {
        UpdateItem(
            state = UpdateItemState(
                downloadId = "preview",
                buildDate = "Apr 27",
                buildVersion = "Clover 4.0",
                status = "Downloading",
                isLocal = false,
                fileSize = "1.1 GB",
                androidUpdateInfo = "Android 17",
                securityUpdate = "Jan 2026",
                installNote = R.string.list_full_install,
                progress = ProgressState.Determinate(
                    percent = 65f,
                    downloadedSize = "715 MB of 1.1 GB",
                    eta = "3 min left",
                ),
                actions = UpdateActions(
                    primary = UpdateAction(
                        type = UpdateActionType.PAUSE_DOWNLOAD,
                    ),
                    secondary = UpdateAction(
                        type = UpdateActionType.CANCEL_DOWNLOAD,
                    ),
                    overflow = listOf(
                        UpdateAction(
                            type = UpdateActionType.VIEW_DOWNLOADS,
                        ),
                    ),
                ),
            ),
            expanded = true,
            onExpandToggle = {},
            onAction = {},
        )
    }
}
