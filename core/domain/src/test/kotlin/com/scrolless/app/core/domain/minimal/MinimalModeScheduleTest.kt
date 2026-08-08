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
package com.scrolless.app.core.domain.minimal

import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.minimal.MinimalModeSchedule
import com.scrolless.app.core.minimal.MinimalModeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

/** 09:00 to 17:00, an ordinary same-day window. */
private val WORK_DAY = MinimalModeWindow(at(9), at(17))

/** 22:00 to 08:00, the one that spans midnight. */
private val NIGHT = MinimalModeWindow(at(22), at(8))

class MinimalModeScheduleTest : BaseTest() {

    @Test
    fun noWindowsIsNeverOpen() {
        assertFalse(MinimalModeSchedule.isOpen(emptyList(), at(3)))
        assertFalse(MinimalModeSchedule.isOpen(emptyList(), at(14)))
    }

    @Test
    fun sameDayWindowIsOpenOnlyInsideIt() {
        val windows = listOf(WORK_DAY)

        assertTrue(MinimalModeSchedule.isOpen(windows, at(12)))
        assertFalse(MinimalModeSchedule.isOpen(windows, at(8, 59)))
        assertFalse(MinimalModeSchedule.isOpen(windows, at(20)))
    }

    /** The start is inclusive and the end exclusive, so the two boundaries differ. */
    @Test
    fun boundaryMinutesFollowTheHalfOpenRule() {
        val windows = listOf(WORK_DAY)

        assertTrue(MinimalModeSchedule.isOpen(windows, at(9)))
        assertTrue(MinimalModeSchedule.isOpen(windows, at(16, 59)))
        assertFalse(MinimalModeSchedule.isOpen(windows, at(17)))
    }

    @Test
    fun windowSpanningMidnightCoversBothSides() {
        val windows = listOf(NIGHT)

        assertTrue(MinimalModeSchedule.isOpen(windows, at(23)))
        assertTrue(MinimalModeSchedule.isOpen(windows, at(0)))
        assertTrue(MinimalModeSchedule.isOpen(windows, at(7, 59)))
        assertFalse(MinimalModeSchedule.isOpen(windows, at(8)))
        assertFalse(MinimalModeSchedule.isOpen(windows, at(12)))
    }

    @Test
    fun equalEndsMeanTheWholeDay() {
        val windows = listOf(MinimalModeWindow(at(6), at(6)))

        assertTrue(MinimalModeSchedule.isOpen(windows, at(6)))
        assertTrue(MinimalModeSchedule.isOpen(windows, at(5, 59)))
        assertTrue(MinimalModeSchedule.isOpen(windows, at(18)))
    }

    @Test
    fun overlappingWindowsAreUnioned() {
        val windows = listOf(MinimalModeWindow(at(9), at(12)), MinimalModeWindow(at(11), at(14)))

        assertTrue(MinimalModeSchedule.isOpen(windows, at(11, 30)))
        assertTrue(MinimalModeSchedule.isOpen(windows, at(13)))
        assertFalse(MinimalModeSchedule.isOpen(windows, at(14)))
    }

    @Test
    fun nextTransitionFromOutsideIsTheWindowStart() {
        assertEquals(
            60 * MinimalModeSchedule.MILLIS_PER_MINUTE,
            MinimalModeSchedule.millisUntilNextTransition(listOf(WORK_DAY), at(8)),
        )
    }

    @Test
    fun nextTransitionFromInsideIsTheWindowEnd() {
        assertEquals(
            5 * 60 * MinimalModeSchedule.MILLIS_PER_MINUTE,
            MinimalModeSchedule.millisUntilNextTransition(listOf(WORK_DAY), at(12)),
        )
    }

    @Test
    fun nextTransitionWrapsAroundMidnight() {
        // 23:00 inside the night window, which closes at 08:00 — nine hours away.
        assertEquals(
            9 * 60 * MinimalModeSchedule.MILLIS_PER_MINUTE,
            MinimalModeSchedule.millisUntilNextTransition(listOf(NIGHT), at(23)),
        )
    }

    /** Nothing ever changes, so the caller is told to check back in a day rather than spin. */
    @Test
    fun scheduleWithoutTransitionsReturnsAFullDay() {
        val fullDay = MinimalModeSchedule.MINUTES_PER_DAY * MinimalModeSchedule.MILLIS_PER_MINUTE

        assertEquals(fullDay, MinimalModeSchedule.millisUntilNextTransition(emptyList(), at(10)))
        assertEquals(
            fullDay,
            MinimalModeSchedule.millisUntilNextTransition(listOf(MinimalModeWindow(0, 0)), at(10)),
        )
    }

    @Test
    fun widerScheduleCoversNarrowerOne() {
        val current = listOf(WORK_DAY)
        val wider = listOf(MinimalModeWindow(at(8), at(18)))

        assertTrue(MinimalModeSchedule.covers(current = current, next = wider))
        assertFalse(MinimalModeSchedule.covers(current = wider, next = current))
    }

    @Test
    fun anIdenticalScheduleCoversItself() {
        assertTrue(MinimalModeSchedule.covers(current = listOf(NIGHT), next = listOf(NIGHT)))
    }

    /** Moving a window sideways keeps the same length but drops minutes, so it is not a cover. */
    @Test
    fun shiftedScheduleOfEqualLengthDoesNotCover() {
        val current = listOf(WORK_DAY)
        val shifted = listOf(MinimalModeWindow(at(10), at(18)))

        assertFalse(MinimalModeSchedule.covers(current = current, next = shifted))
    }
}
