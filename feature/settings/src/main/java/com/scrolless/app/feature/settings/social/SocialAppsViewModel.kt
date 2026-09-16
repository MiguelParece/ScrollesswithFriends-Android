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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.repository.InstalledApp
import com.scrolless.app.core.repository.InstalledAppsProvider
import com.scrolless.app.core.repository.SocialBlocklistStore
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
class SocialAppsViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val socialBlocklistStore: SocialBlocklistStore,
    private val installedAppsProvider: InstalledAppsProvider,
    private val strictModeManager: StrictModeManager,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledApp>?>(null)

    val uiState: StateFlow<SocialAppsUiState> = combine(
        userSettingsStore.getActiveBlockOption(),
        socialBlocklistStore.getBlockedApps(),
        strictModeManager.observeState(),
        installedApps,
    ) { blockOption, blockedApps, strictState, apps ->
        SocialAppsUiState(
            socialModeSelected = blockOption == BlockOption.SocialMedia,
            blockedApps = blockedApps,
            installedApps = apps.orEmpty(),
            loadingApps = apps == null,
            strictModeArmed = strictModeManager.isArmed(strictState),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SocialAppsUiState(),
        )

    init {
        viewModelScope.launch {
            installedApps.value = installedAppsProvider.launchableApps()
        }
    }

    fun onAppBlockedChange(packageId: String, blocked: Boolean) {
        // Taking an app off the list lets it open again; adding one only tightens.
        if (!blocked && !StrictModeGuard.canUnblockSocialApp(uiState.value.strictModeArmed)) return
        viewModelScope.launch {
            if (blocked) socialBlocklistStore.blockApp(packageId) else socialBlocklistStore.unblockApp(packageId)
        }
    }
}

data class SocialAppsUiState(
    /** Whether Social Media is the active mode, which is what puts this list in force. */
    val socialModeSelected: Boolean = false,
    val blockedApps: Set<String> = emptySet(),
    val installedApps: List<InstalledApp> = emptyList(),
    val loadingApps: Boolean = true,
    val strictModeArmed: Boolean = false,
)
