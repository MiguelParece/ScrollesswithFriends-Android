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
package com.scrolless.app.core.strict

import com.scrolless.app.core.model.StrictArmResult
import com.scrolless.app.core.model.StrictModeState
import kotlinx.coroutines.flow.Flow

/**
 * Owns the strict-mode time lock: while armed, the accessibility service closes any
 * system screen that could disable or uninstall Scrolless. There is no early exit —
 * the lock ends only when its timer runs out.
 */
interface StrictModeManager {

    /** Combined flow of the persisted lock state; collected by the service and UI. */
    fun observeState(): Flow<StrictModeState>

    /** Pure and synchronous — safe to call from the accessibility event thread. */
    fun isArmed(state: StrictModeState): Boolean

    /** Trusted remaining lock time; 0 when disarmed. */
    fun remainingMillis(state: StrictModeState): Long

    /**
     * Arms the lock, or extends it forward. Durations are clamped to [MAX_DURATION_MILLIS];
     * an extension shorter than the trusted remaining time is rejected.
     */
    suspend fun arm(durationMillis: Long): StrictArmResult

    /**
     * Rewrites the anchors after a legitimate reboot so elapsed-based enforcement
     * resumes. Idempotent; never re-anchors on a suspicious clock.
     */
    suspend fun reanchorIfNeeded()

    companion object {
        const val MAX_DURATION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
