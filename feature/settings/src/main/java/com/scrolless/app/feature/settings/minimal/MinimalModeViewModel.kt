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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.minimal.MinimalModeWindow
import com.scrolless.app.core.repository.InstalledApp
import com.scrolless.app.core.repository.InstalledAppsProvider
import com.scrolless.app.core.repository.MinimalModeStore
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.strict.StrictModeGuard
import com.scrolless.app.core.strict.StrictModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MinimalModeViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val minimalModeStore: MinimalModeStore,
    private val installedAppsProvider: InstalledAppsProvider,
    private val strictModeManager: StrictModeManager,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledApp>?>(null)

    val uiState: StateFlow<MinimalModeUiState> = combine(
        userSettingsStore.getMinimalModeEnabled(),
        minimalModeStore.getWindows(),
        minimalModeStore.getAllowedApps(),
        strictModeManager.observeState(),
        installedApps,
    ) { enabled, windows, allowedApps, strictState, apps ->
        MinimalModeUiState(
            enabled = enabled,
            windows = windows,
            allowedApps = allowedApps,
            installedApps = apps.orEmpty(),
            loadingApps = apps == null,
            strictModeArmed = strictModeManager.isArmed(strictState),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MinimalModeUiState(),
        )

    init {
        viewModelScope.launch {
            installedApps.value = installedAppsProvider.launchableApps()
        }
    }

    fun onEnabledChange(checked: Boolean) {
        // Switching minimal mode off removes protection; strict mode only allows switching on.
        if (!StrictModeGuard.canChangeMinimalModeEnabled(uiState.value.strictModeArmed, checked)) return
        viewModelScope.launch {
            userSettingsStore.setMinimalModeEnabled(checked)
        }
    }

    fun onAppAllowedChange(packageId: String, allowed: Boolean) {
        // Letting one more app through is a weakening; taking one away is not.
        if (allowed && !StrictModeGuard.canAddAllowedApp(uiState.value.strictModeArmed)) return
        viewModelScope.launch {
            if (allowed) minimalModeStore.allowApp(packageId) else minimalModeStore.disallowApp(packageId)
        }
    }

    fun onAddWindow(startMinuteOfDay: Int, endMinuteOfDay: Int) {
        val next = uiState.value.windows + MinimalModeWindow(startMinuteOfDay, endMinuteOfDay)
        setWindows(next)
    }

    fun onRemoveWindow(index: Int) {
        val current = uiState.value.windows
        if (index !in current.indices) return
        setWindows(current.filterIndexed { position, _ -> position != index })
    }

    private fun setWindows(next: List<MinimalModeWindow>) {
        val current = uiState.value.windows
        // The schedule may grow but never shrink while the lock is armed.
        if (!StrictModeGuard.canChangeMinimalModeSchedule(uiState.value.strictModeArmed, current, next)) return
        viewModelScope.launch {
            minimalModeStore.setWindows(next)
        }
    }
}

data class MinimalModeUiState(
    val enabled: Boolean = false,
    val windows: List<MinimalModeWindow> = emptyList(),
    val allowedApps: Set<String> = emptySet(),
    val installedApps: List<InstalledApp> = emptyList(),
    val loadingApps: Boolean = true,
    val strictModeArmed: Boolean = false,
)
