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
package com.scrolless.app.core.blocking.handler

import com.scrolless.app.core.blocking.time.ClockAnomalyDetector
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.PartnerQuotaState
import com.scrolless.app.core.model.QuotaWindow
import java.time.LocalDateTime
import timber.log.Timber

/**
 * Blocks content once the baseline allowance (plus any partner grants) for the current
 * quota window is exhausted.
 *
 * Each day has three windows (see [QuotaWindow]); crossing into a new window resets usage
 * and grants. Rollover is only honoured when the wall clock is consistent with elapsed
 * realtime — a manipulated clock carries the counters forward instead of refilling them,
 * so moving the clock can never produce fresh allowance.
 */
class PartnerQuotaBlockHandler(
    initialState: PartnerQuotaState,
    private val onStateChanged: (PartnerQuotaState) -> Unit,
    private val timeProvider: TimeProvider,
    private val baselineMillis: Long = DEFAULT_BASELINE_MILLIS,
) : BlockOptionHandler {

    private var state: PartnerQuotaState = initialState
    private var sessionUsageBase: Long = initialState.usedMillis
    private var sessionElapsedOffset: Long = 0L

    private val limitMillis: Long
        get() = baselineMillis + state.grantedMillis

    /**
     * Re-anchor the clock check and roll the window over when the boundary was crossed.
     *
     * @param sessionElapsedTime Elapsed session time when called mid-session, so a trusted
     *   rollover can exclude pre-boundary usage from the new window.
     */
    private fun ensureWindowFresh(sessionElapsedTime: Long?) {
        val wall = timeProvider.currentTimeInMillis()
        val elapsed = timeProvider.elapsedRealtimeMillis()
        val boot = timeProvider.bootCount()

        val verdict = ClockAnomalyDetector.evaluate(
            anchorWallMillis = state.anchorWallMillis,
            anchorElapsedMillis = state.anchorElapsedMillis,
            anchorBootCount = state.anchorBootCount,
            wallMillis = wall,
            elapsedMillis = elapsed,
            bootCount = boot,
        )

        val currentKey = windowKeyFor(timeProvider.localDateTimeNow())
        val newState = when {
            state.windowKey == currentKey -> state

            verdict == ClockAnomalyDetector.Verdict.SUSPICIOUS -> {
                // Window label follows the visible clock, but counters are never refilled
                // on an untrusted jump: quota can be lost, not regained, by moving the clock.
                Timber.w("PartnerQuota: suspicious clock, carrying counters into %s", currentKey)
                state.copy(windowKey = currentKey)
            }

            else -> {
                Timber.d("PartnerQuota: window rollover %s -> %s", state.windowKey, currentKey)
                sessionUsageBase = 0L
                if (sessionElapsedTime != null) {
                    sessionElapsedOffset = sessionElapsedTime
                }
                state.copy(windowKey = currentKey, usedMillis = 0L, grantedMillis = 0L)
            }
        }.copy(anchorWallMillis = wall, anchorElapsedMillis = elapsed, anchorBootCount = boot)

        // Anchors always advance in memory; persistence happens on counter/window changes only.
        val persist = newState.windowKey != state.windowKey ||
            newState.usedMillis != state.usedMillis ||
            newState.grantedMillis != state.grantedMillis
        state = newState
        if (persist) {
            onStateChanged(newState)
        }
    }

    override fun onEnterContent(currentDailyUsage: Long): Boolean {
        ensureWindowFresh(sessionElapsedTime = null)
        sessionUsageBase = state.usedMillis
        sessionElapsedOffset = 0L

        val shouldBlock = state.usedMillis >= limitMillis
        Timber.d(
            "PartnerQuota.onEnter: window=%s usage=%d/%d -> block=%s",
            state.windowKey,
            state.usedMillis,
            limitMillis,
            shouldBlock,
        )
        return shouldBlock
    }

    override fun onPeriodicCheck(currentDailyUsage: Long, elapsedTime: Long): BlockingResult {
        ensureWindowFresh(sessionElapsedTime = elapsedTime)

        val projectedUsage = sessionUsageBase + (elapsedTime - sessionElapsedOffset)
        return if (projectedUsage >= limitMillis) {
            val clamped = limitMillis
            if (state.usedMillis != clamped) {
                state = state.copy(usedMillis = clamped)
                onStateChanged(state)
            }
            sessionUsageBase = clamped
            sessionElapsedOffset = elapsedTime
            Timber.v("PartnerQuota.onPeriodic: projected=%d/%d -> block", projectedUsage, limitMillis)
            BlockingResult.BlockNow
        } else {
            BlockingResult.CheckLater(limitMillis - projectedUsage)
        }
    }

    override fun onExitContent(sessionTime: Long) {
        if (sessionTime <= 0L) {
            Timber.v("PartnerQuota.onExit: ignore non-positive session=%d", sessionTime)
            return
        }

        ensureWindowFresh(sessionElapsedTime = sessionTime)

        val sessionContribution = (sessionTime - sessionElapsedOffset).coerceAtLeast(0L)
        val updatedUsage = (sessionUsageBase + sessionContribution).coerceAtMost(limitMillis)
        if (updatedUsage != state.usedMillis) {
            Timber.v("PartnerQuota.onExit: +%d -> usage=%d", sessionContribution, updatedUsage)
            state = state.copy(usedMillis = updatedUsage)
            onStateChanged(state)
        }
        sessionUsageBase = updatedUsage
        sessionElapsedOffset = 0L
    }

    companion object {
        const val DEFAULT_BASELINE_MILLIS = 15 * 60_000L

        private const val MORNING_START_HOUR = 5
        private const val AFTERNOON_START_HOUR = 12
        private const val NIGHT_START_HOUR = 18

        fun windowFor(dateTime: LocalDateTime): QuotaWindow = when (dateTime.hour) {
            in MORNING_START_HOUR until AFTERNOON_START_HOUR -> QuotaWindow.MORNING
            in AFTERNOON_START_HOUR until NIGHT_START_HOUR -> QuotaWindow.AFTERNOON
            else -> QuotaWindow.NIGHT
        }

        /**
         * Key of the window containing [dateTime]. Hours before 05:00 belong to the previous
         * day's night window, so the date part is the day the window started.
         */
        fun windowKeyFor(dateTime: LocalDateTime): String {
            val window = windowFor(dateTime)
            val windowDate = if (dateTime.hour < MORNING_START_HOUR) {
                dateTime.toLocalDate().minusDays(1)
            } else {
                dateTime.toLocalDate()
            }
            return "$windowDate|$window"
        }
    }
}
