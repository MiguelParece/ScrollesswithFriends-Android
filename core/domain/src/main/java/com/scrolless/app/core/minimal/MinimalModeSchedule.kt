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
package com.scrolless.app.core.minimal

/**
 * A stretch of the day during which the phone is reduced to the allowed apps.
 *
 * Both ends are minutes since midnight in `0..1439`. The start is inclusive and the end is
 * exclusive. An end that is not after the start means the window spans midnight — 22:00 to
 * 08:00 is `start = 1320, end = 480` — the same convention as the Partner Quota night
 * window. Equal ends therefore mean the whole day.
 */
data class MinimalModeWindow(val startMinuteOfDay: Int, val endMinuteOfDay: Int) {

    fun contains(minuteOfDay: Int): Boolean = if (endMinuteOfDay > startMinuteOfDay) {
        minuteOfDay >= startMinuteOfDay && minuteOfDay < endMinuteOfDay
    } else {
        minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
    }

    /** Every minute this window covers. Used to compare schedules for strictness. */
    fun coveredMinutes(): Set<Int> = (0 until MinimalModeSchedule.MINUTES_PER_DAY)
        .filterTo(mutableSetOf(), ::contains)
}

/**
 * Decides whether minimal mode is in force right now, and when that answer next changes.
 *
 * Pure and minute-granular. Callers hold the clock; see
 * [com.scrolless.app.core.blocking.time.TrustedWallClock] for reading one that a user
 * cannot move.
 */
object MinimalModeSchedule {

    const val MINUTES_PER_DAY = 24 * 60
    const val MILLIS_PER_MINUTE = 60_000L

    fun isOpen(windows: List<MinimalModeWindow>, minuteOfDay: Int): Boolean = windows.any { it.contains(minuteOfDay) }

    /**
     * Millis from the start of [minuteOfDay] until [isOpen] flips.
     *
     * Walks the day a minute at a time rather than doing boundary arithmetic: windows may
     * overlap, may span midnight and may cover everything, and a scan is obviously correct
     * where the closed form is easy to get subtly wrong. It runs at most 1440 cheap checks
     * and only when a transition is being scheduled.
     *
     * The result is measured from the start of the current minute, so a caller that wants a
     * delay from *now* must subtract however far into the minute it already is. With no
     * windows, or with windows covering the whole day, there is no transition and a full day
     * is returned.
     */
    fun millisUntilNextTransition(windows: List<MinimalModeWindow>, minuteOfDay: Int): Long {
        val openNow = isOpen(windows, minuteOfDay)
        for (offset in 1..MINUTES_PER_DAY) {
            val minute = (minuteOfDay + offset) % MINUTES_PER_DAY
            if (isOpen(windows, minute) != openNow) return offset * MILLIS_PER_MINUTE
        }
        return MINUTES_PER_DAY * MILLIS_PER_MINUTE
    }

    /** Every minute of the day covered by any of [windows]. */
    fun coveredMinutes(windows: List<MinimalModeWindow>): Set<Int> = windows.flatMapTo(mutableSetOf()) { it.coveredMinutes() }

    /**
     * Whether [next] keeps every minute [current] already covered. Shrinking protection is
     * what strict mode refuses; widening it is always allowed.
     */
    fun covers(current: List<MinimalModeWindow>, next: List<MinimalModeWindow>): Boolean =
        coveredMinutes(next).containsAll(coveredMinutes(current))
}
