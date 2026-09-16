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
package com.scrolless.app.feature.settings.social

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.feature.settings.R
import com.scrolless.app.feature.settings.apps.AppPickerRow
import com.scrolless.app.feature.settings.apps.AppPickerScaffold
import com.scrolless.app.feature.settings.apps.AppPickerStatusCard

@Composable
fun SocialAppsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SocialAppsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SocialAppsScreenContent(
        modifier = modifier,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAppBlockedChange = viewModel::onAppBlockedChange,
    )
}

@Composable
private fun SocialAppsScreenContent(
    uiState: SocialAppsUiState,
    onNavigateBack: () -> Unit,
    onAppBlockedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppPickerScaffold(
        title = stringResource(R.string.social_apps_title),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        item {
            AppPickerStatusCard(
                status = if (uiState.socialModeSelected) {
                    stringResource(R.string.app_picker_status_on)
                } else {
                    stringResource(R.string.app_picker_status_off)
                },
                active = uiState.socialModeSelected,
                description = stringResource(R.string.social_apps_description),
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
            val blocked = app.packageId in uiState.blockedApps
            AppPickerRow(
                app = app,
                checked = blocked,
                // Unticking lets an app back in, so strict mode freezes that direction only.
                enabled = !blocked || !uiState.strictModeArmed,
                onCheckedChange = { onAppBlockedChange(app.packageId, it) },
            )
        }
    }
}
