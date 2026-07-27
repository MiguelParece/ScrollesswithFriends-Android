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
package com.scrolless.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.strict.StrictModeGuard
import com.scrolless.app.core.strict.StrictModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsStore: UserSettingsStore,
    private val strictModeManager: StrictModeManager,
) : ViewModel() {

    // Note: strict-mode fields are recomputed on emissions and lifecycle restarts, not on a
    // ticking clock — the display won't flip to "off" at the exact expiry instant.
    val uiState: StateFlow<SettingsUiState> = combine(
        userSettingsStore.getPauseDuration(),
        userSettingsStore.getExceptReelsSentByDm(),
        userSettingsStore.getTimerOverlayEnabled(),
        strictModeManager.observeState(),
        userSettingsStore.getInstagramFeedBlockingEnabled(),
    ) { pauseDurationMillis, exceptReelsSentByDm, timerOverlayEnabled, strictState, instagramFeedBlockingEnabled ->
        SettingsUiState(
            pauseDurationMinutes = (pauseDurationMillis / 60_000L).toInt().coerceIn(1, 60),
            exceptReelsSentByDm = exceptReelsSentByDm,
            timerOverlayEnabled = timerOverlayEnabled,
            instagramFeedBlockingEnabled = instagramFeedBlockingEnabled,
            strictModeArmed = strictModeManager.isArmed(strictState),
            strictModeUntilMillis = strictState.untilAtMillis,
            strictModeRemainingMillis = strictModeManager.remainingMillis(strictState),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun onPauseDurationChange(minutes: Int) {
        viewModelScope.launch {
            userSettingsStore.setPauseDuration(minutes * 60_000L)
        }
    }

    fun onExceptReelsSentByDmChange(checked: Boolean) {
        // Exempting DM reels removes protection, so strict mode only allows switching it off.
        if (!StrictModeGuard.canChangeExceptReelsSentByDm(uiState.value.strictModeArmed, checked)) {
            return
        }
        viewModelScope.launch {
            userSettingsStore.setExceptReelsSentByDm(checked)
        }
    }

    fun onInstagramFeedBlockingEnabledChange(checked: Boolean) {
        // Switching feed blocking off removes protection; strict mode only allows switching it on.
        if (!StrictModeGuard.canChangeInstagramFeedBlocking(uiState.value.strictModeArmed, checked)) {
            return
        }
        viewModelScope.launch {
            userSettingsStore.setInstagramFeedBlockingEnabled(checked)
        }
    }

    fun onTimerOverlayEnabledChange(checked: Boolean) {
        viewModelScope.launch {
            userSettingsStore.setTimerOverlayToggle(checked)
        }
    }

    fun onArmStrictMode(durationMillis: Long) {
        viewModelScope.launch {
            strictModeManager.arm(durationMillis)
        }
    }
}

data class SettingsUiState(
    val pauseDurationMinutes: Int = 5,
    val exceptReelsSentByDm: Boolean = false,
    val timerOverlayEnabled: Boolean = false,
    val instagramFeedBlockingEnabled: Boolean = false,
    val strictModeArmed: Boolean = false,
    val strictModeUntilMillis: Long = 0L,
    val strictModeRemainingMillis: Long = 0L,
)
