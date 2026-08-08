/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.feature.settings.minimal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.core.minimal.MinimalModeWindow
import com.scrolless.app.core.repository.InstalledApp
import com.scrolless.app.designsystem.util.rememberHapticHelper
import com.scrolless.app.feature.settings.R

@Composable
fun MinimalModeScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MinimalModeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MinimalModeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEnabledChange = viewModel::onEnabledChange,
        onAddWindow = viewModel::onAddWindow,
        onRemoveWindow = viewModel::onRemoveWindow,
        onAppAllowedChange = viewModel::onAppAllowedChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalModeScreenContent(
    uiState: MinimalModeUiState,
    onNavigateBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onAddWindow: (Int, Int) -> Unit,
    onRemoveWindow: (Int) -> Unit,
    onAppAllowedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticHelper = rememberHapticHelper()
    var showWindowPicker by remember { mutableStateOf(false) }

    // Switching the mode off and shrinking the schedule both weaken protection.
    val lockedOff = uiState.strictModeArmed && uiState.enabled
    val lockedSchedule = uiState.strictModeArmed

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.minimal_mode_title),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            hapticHelper.playTick()
                            onNavigateBack()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 28.dp),
        ) {
            item {
                MinimalModeCard {
                    MinimalModeSwitchRow(
                        title = stringResource(R.string.minimal_mode_enable_title),
                        description = if (lockedOff) {
                            stringResource(R.string.settings_locked_by_strict_mode)
                        } else {
                            stringResource(R.string.minimal_mode_enable_description)
                        },
                        checked = uiState.enabled,
                        enabled = !lockedOff,
                        onCheckedChange = onEnabledChange,
                    )
                }
            }

            item { MinimalModeSectionLabel(stringResource(R.string.minimal_mode_schedule_section)) }

            item {
                MinimalModeCard {
                    Column {
                        if (uiState.windows.isEmpty()) {
                            Text(
                                text = stringResource(R.string.minimal_mode_no_windows),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                        uiState.windows.forEachIndexed { index, window ->
                            WindowRow(
                                window = window,
                                canRemove = !lockedSchedule,
                                onRemove = { onRemoveWindow(index) },
                            )
                        }
                        Button(
                            onClick = {
                                hapticHelper.playTick()
                                showWindowPicker = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Text(stringResource(R.string.minimal_mode_add_window))
                        }
                    }
                }
            }

            item { MinimalModeSectionLabel(stringResource(R.string.minimal_mode_apps_section)) }

            item {
                Text(
                    text = stringResource(R.string.minimal_mode_apps_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (uiState.loadingApps) {
                item {
                    Text(
                        text = stringResource(R.string.minimal_mode_loading_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    )
                }
            }

            items(uiState.installedApps, key = { it.packageId }) { app ->
                val allowed = app.packageId in uiState.allowedApps
                AppRow(
                    app = app,
                    allowed = allowed,
                    // Removing an app always tightens, so only adding is locked.
                    enabled = allowed || !uiState.strictModeArmed,
                    onAllowedChange = { onAppAllowedChange(app.packageId, it) },
                )
            }
        }
    }

    if (showWindowPicker) {
        WindowPickerDialog(
            onDismiss = { showWindowPicker = false },
            onConfirm = { start, end ->
                showWindowPicker = false
                onAddWindow(start, end)
            },
        )
    }
}

@Composable
private fun MinimalModeCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

@Composable
private fun MinimalModeSectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 2.dp, top = 10.dp),
    )
}

@Composable
private fun MinimalModeSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticHelper = rememberHapticHelper()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { isOn ->
                hapticHelper.playToggle(isOn)
                onCheckedChange(isOn)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.size(width = 58.dp, height = 36.dp),
        )
    }
}

@Composable
private fun WindowRow(window: MinimalModeWindow, canRemove: Boolean, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val hapticHelper = rememberHapticHelper()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.minimal_mode_window_range,
                    formatMinuteOfDay(window.startMinuteOfDay),
                    formatMinuteOfDay(window.endMinuteOfDay),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (window.endMinuteOfDay <= window.startMinuteOfDay) {
                Text(
                    text = stringResource(R.string.minimal_mode_window_overnight),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(
            enabled = canRemove,
            onClick = {
                hapticHelper.playTick()
                onRemove()
            },
        ) {
            Text(stringResource(R.string.minimal_mode_remove_window))
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    allowed: Boolean,
    enabled: Boolean,
    onAllowedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticHelper = rememberHapticHelper()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                hapticHelper.playToggle(!allowed)
                onAllowedChange(!allowed)
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = allowed,
            enabled = enabled,
            onCheckedChange = { isOn ->
                hapticHelper.playToggle(isOn)
                onAllowedChange(isOn)
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Asks for the start time, then the end time, in one dialog.
 *
 * Two steps rather than two dialogs so the flow can be cancelled without leaving a
 * half-created window behind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WindowPickerDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var pickingEnd by remember { mutableStateOf(false) }
    var startMinute by remember { mutableIntStateOf(0) }
    val timePickerState = rememberTimePickerState(initialHour = 22, initialMinute = 0, is24Hour = true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (pickingEnd) {
                        stringResource(R.string.minimal_mode_pick_end)
                    } else {
                        stringResource(R.string.minimal_mode_pick_start)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            val picked = timePickerState.hour * 60 + timePickerState.minute
                            if (pickingEnd) {
                                onConfirm(startMinute, picked)
                            } else {
                                startMinute = picked
                                pickingEnd = true
                            }
                        },
                    ) {
                        Text(
                            text = if (pickingEnd) {
                                stringResource(R.string.minimal_mode_save_window)
                            } else {
                                stringResource(R.string.minimal_mode_next)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "%02d:%02d".format(hour, minute)
}
