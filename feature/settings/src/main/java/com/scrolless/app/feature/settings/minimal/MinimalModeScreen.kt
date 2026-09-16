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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.core.minimal.MinimalModeWindow
import com.scrolless.app.designsystem.util.rememberHapticHelper
import com.scrolless.app.feature.settings.R
import com.scrolless.app.feature.settings.apps.AppPickerCard
import com.scrolless.app.feature.settings.apps.AppPickerRow
import com.scrolless.app.feature.settings.apps.AppPickerScaffold
import com.scrolless.app.feature.settings.apps.AppPickerSectionLabel
import com.scrolless.app.feature.settings.apps.AppPickerStatusCard

@Composable
fun MinimalModeScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MinimalModeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MinimalModeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddWindow = viewModel::onAddWindow,
        onRemoveWindow = viewModel::onRemoveWindow,
        onAppAllowedChange = viewModel::onAppAllowedChange,
    )
}

@Composable
private fun MinimalModeScreenContent(
    uiState: MinimalModeUiState,
    onNavigateBack: () -> Unit,
    onAddWindow: (Int, Int) -> Unit,
    onRemoveWindow: (Int) -> Unit,
    onAppAllowedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticHelper = rememberHapticHelper()
    var showWindowPicker by remember { mutableStateOf(false) }

    AppPickerScaffold(
        title = stringResource(R.string.minimal_mode_title),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        item {
            AppPickerStatusCard(
                status = if (uiState.allowlistModeSelected) {
                    stringResource(R.string.app_picker_status_on)
                } else {
                    stringResource(R.string.app_picker_status_off)
                },
                active = uiState.allowlistModeSelected,
                description = stringResource(R.string.minimal_mode_enable_description),
            )
        }

        item { AppPickerSectionLabel(stringResource(R.string.minimal_mode_schedule_section)) }

        item {
            AppPickerCard {
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
                            // Shrinking the schedule weakens protection, so it is frozen.
                            canRemove = !uiState.strictModeArmed,
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

        item { AppPickerSectionLabel(stringResource(R.string.minimal_mode_apps_section)) }

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
                    text = stringResource(R.string.app_picker_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }

        items(uiState.installedApps, key = { it.packageId }) { app ->
            val allowed = app.packageId in uiState.allowedApps
            AppPickerRow(
                app = app,
                checked = allowed,
                // Removing an app always tightens, so only adding is locked.
                enabled = allowed || !uiState.strictModeArmed,
                onCheckedChange = { onAppAllowedChange(app.packageId, it) },
            )
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
