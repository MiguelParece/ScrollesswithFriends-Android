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
package com.scrolless.app.feature.settings.strict

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scrolless.app.feature.settings.R
import java.util.concurrent.TimeUnit

private val PRESETS = listOf(
    TimeUnit.HOURS.toMillis(1),
    TimeUnit.DAYS.toMillis(1),
    TimeUnit.DAYS.toMillis(3),
    TimeUnit.DAYS.toMillis(7),
    TimeUnit.DAYS.toMillis(30),
)

/**
 * Arms (or forward-extends) the strict-mode time lock. Presets not longer than the
 * current remaining time are disabled — the lock can never be shortened.
 */
@Composable
fun StrictModeDialog(armed: Boolean, remainingMillis: Long, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    var selectedMillis by remember {
        mutableStateOf(PRESETS.firstOrNull { !armed || it > remainingMillis })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (armed) R.string.strict_dialog_title_extend else R.string.strict_dialog_title_arm,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.strict_dialog_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                PRESETS.forEach { presetMillis ->
                    val enabled = !armed || presetMillis > remainingMillis
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { selectedMillis = presetMillis }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMillis == presetMillis,
                            onClick = { selectedMillis = presetMillis },
                            enabled = enabled,
                        )
                        Text(
                            text = presetLabel(presetMillis),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedMillis?.let(onConfirm) },
                enabled = selectedMillis != null,
            ) {
                Text(
                    stringResource(
                        if (armed) R.string.strict_dialog_confirm_extend else R.string.strict_dialog_confirm_arm,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun presetLabel(presetMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(presetMillis)
    return if (hours < 24) {
        pluralStringResource(R.plurals.strict_preset_hours, hours.toInt(), hours.toInt())
    } else {
        val days = TimeUnit.MILLISECONDS.toDays(presetMillis).toInt()
        pluralStringResource(R.plurals.strict_preset_days, days, days)
    }
}
