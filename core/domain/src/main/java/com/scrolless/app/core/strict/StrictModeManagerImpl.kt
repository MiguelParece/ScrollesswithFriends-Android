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

import com.scrolless.app.core.blocking.time.ClockAnomalyDetector
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.StrictArmResult
import com.scrolless.app.core.model.StrictModeState
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.strict.StrictModeManager.Companion.MAX_DURATION_MILLIS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import timber.log.Timber

class StrictModeManagerImpl(private val userSettingsStore: UserSettingsStore, private val timeProvider: TimeProvider) : StrictModeManager {

    override fun observeState(): Flow<StrictModeState> = combine(
        userSettingsStore.getStrictUntil(),
        userSettingsStore.getStrictAnchorWall(),
        userSettingsStore.getStrictAnchorElapsed(),
        userSettingsStore.getStrictAnchorBoot(),
    ) { untilAt, anchorWall, anchorElapsed, anchorBoot ->
        StrictModeState(
            untilAtMillis = untilAt,
            anchorWallMillis = anchorWall,
            anchorElapsedMillis = anchorElapsed,
            anchorBootCount = anchorBoot,
        )
    }

    /**
     * Within the anchor's boot, expiry is measured purely against elapsed realtime, so
     * clock edits cannot end (or stretch) the lock. Across a reboot elapsed resets and
     * the wall clock is used, gated by [ClockAnomalyDetector]: a provably rolled-back
     * clock keeps the lock armed; an unverifiable forward clock is accepted.
     */
    override fun isArmed(state: StrictModeState): Boolean {
        if (state.untilAtMillis <= 0L) return false

        val wall = timeProvider.currentTimeInMillis()
        val elapsed = timeProvider.elapsedRealtimeMillis()
        val boot = timeProvider.bootCount()

        if (isSameBoot(state, elapsed, boot)) {
            return (elapsed - state.anchorElapsedMillis) < (state.untilAtMillis - state.anchorWallMillis)
        }

        val verdict = ClockAnomalyDetector.evaluate(
            anchorWallMillis = state.anchorWallMillis,
            anchorElapsedMillis = state.anchorElapsedMillis,
            anchorBootCount = state.anchorBootCount,
            wallMillis = wall,
            elapsedMillis = elapsed,
            bootCount = boot,
        )
        return when (verdict) {
            ClockAnomalyDetector.Verdict.SUSPICIOUS -> true

            // fail closed
            else -> wall < state.untilAtMillis
        }
    }

    override fun remainingMillis(state: StrictModeState): Long {
        if (!isArmed(state)) return 0L

        val elapsed = timeProvider.elapsedRealtimeMillis()
        val boot = timeProvider.bootCount()
        val remaining = if (isSameBoot(state, elapsed, boot)) {
            (state.untilAtMillis - state.anchorWallMillis) - (elapsed - state.anchorElapsedMillis)
        } else {
            state.untilAtMillis - timeProvider.currentTimeInMillis()
        }
        return remaining.coerceAtLeast(0L)
    }

    override suspend fun arm(durationMillis: Long): StrictArmResult {
        if (durationMillis <= 0L) return StrictArmResult.RejectedInvalidDuration
        val clamped = durationMillis.coerceAtMost(MAX_DURATION_MILLIS)

        val current = currentState()
        // Compare against the trusted remaining time, not wall end times: a rolled-back
        // wall clock would otherwise let an "extension" end earlier than the current lock.
        if (isArmed(current) && clamped <= remainingMillis(current)) {
            return StrictArmResult.RejectedWouldShorten
        }

        val wall = timeProvider.currentTimeInMillis()
        val untilAt = wall + clamped
        userSettingsStore.updateStrictModeState(
            strictUntilAt = untilAt,
            anchorWallMillis = wall,
            anchorElapsedMillis = timeProvider.elapsedRealtimeMillis(),
            anchorBootCount = timeProvider.bootCount(),
        )
        Timber.i("Strict mode armed for %d ms (until %d)", clamped, untilAt)
        return StrictArmResult.Armed(untilAtMillis = untilAt)
    }

    override suspend fun reanchorIfNeeded() {
        val current = currentState()
        if (!isArmed(current)) return

        val elapsed = timeProvider.elapsedRealtimeMillis()
        val boot = timeProvider.bootCount()
        if (isSameBoot(current, elapsed, boot)) return

        val wall = timeProvider.currentTimeInMillis()
        val verdict = ClockAnomalyDetector.evaluate(
            anchorWallMillis = current.anchorWallMillis,
            anchorElapsedMillis = current.anchorElapsedMillis,
            anchorBootCount = current.anchorBootCount,
            wallMillis = wall,
            elapsedMillis = elapsed,
            bootCount = boot,
        )
        // Never re-anchor on a suspicious clock: a rolled-back wall would inflate
        // (untilAt - anchorWall) and stretch the lock indefinitely.
        if (verdict == ClockAnomalyDetector.Verdict.SUSPICIOUS) {
            Timber.w("Strict mode: skipping re-anchor on suspicious clock")
            return
        }

        userSettingsStore.updateStrictModeState(
            strictUntilAt = current.untilAtMillis,
            anchorWallMillis = wall,
            anchorElapsedMillis = elapsed,
            anchorBootCount = boot,
        )
        Timber.d("Strict mode re-anchored after reboot")
    }

    private fun isSameBoot(state: StrictModeState, elapsed: Long, boot: Int): Boolean = if (state.anchorBootCount >= 0 && boot >= 0) {
        state.anchorBootCount == boot && elapsed >= state.anchorElapsedMillis
    } else {
        elapsed >= state.anchorElapsedMillis
    }

    private suspend fun currentState() = StrictModeState(
        untilAtMillis = userSettingsStore.getStrictUntil().first(),
        anchorWallMillis = userSettingsStore.getStrictAnchorWall().first(),
        anchorElapsedMillis = userSettingsStore.getStrictAnchorElapsed().first(),
        anchorBootCount = userSettingsStore.getStrictAnchorBoot().first(),
    )
}
