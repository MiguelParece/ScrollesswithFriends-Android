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
package com.scrolless.app.core.domain.strict

import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.minimal.MinimalModeWindow
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.strict.StrictModeGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L

class StrictModeGuardTest : BaseTest() {

    @Test
    fun disarmed_everythingIsAllowed() {
        assertTrue(StrictModeGuard.canChangeBlockOption(armed = false, BlockOption.BlockAll, BlockOption.NothingSelected))
        assertTrue(StrictModeGuard.canChangeTimeLimit(armed = false, currentMillis = MINUTE, nextMillis = 60 * MINUTE))
        assertTrue(StrictModeGuard.canPause(armed = false, shouldPause = true))
        assertTrue(StrictModeGuard.canChangeExceptReelsSentByDm(armed = false, next = true))
        assertTrue(StrictModeGuard.canChangeInstagramFeedBlocking(armed = false, next = false))
        assertTrue(
            StrictModeGuard.canChangeIntervalConfig(
                armed = false,
                currentAllowanceMillis = 5 * MINUTE,
                currentIntervalMillis = 60 * MINUTE,
                nextAllowanceMillis = 30 * MINUTE,
                nextIntervalMillis = 30 * MINUTE,
            ),
        )
    }

    @Test
    fun armed_turningBlockingOffIsRefused() {
        BlockOption.entries.filter { it != BlockOption.NothingSelected }.forEach { current ->
            assertFalse(
                "should not be able to disable blocking from $current",
                StrictModeGuard.canChangeBlockOption(armed = true, current, BlockOption.NothingSelected),
            )
        }
    }

    @Test
    fun armed_movingToWeakerOptionIsRefused() {
        assertFalse(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.BlockAll, BlockOption.DailyLimit))
        assertFalse(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.PartnerQuota, BlockOption.IntervalTimer))
        assertFalse(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.IntervalTimer, BlockOption.DailyLimit))
    }

    @Test
    fun armed_movingToStrongerOrSameOptionIsAllowed() {
        assertTrue(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.DailyLimit, BlockOption.BlockAll))
        assertTrue(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.IntervalTimer, BlockOption.PartnerQuota))
        assertTrue(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.BlockAll, BlockOption.BlockAll))
        // Starting from nothing selected, any real option is an improvement.
        assertTrue(StrictModeGuard.canChangeBlockOption(armed = true, BlockOption.NothingSelected, BlockOption.DailyLimit))
    }

    @Test
    fun armed_timeLimitCanOnlyShrink() {
        assertFalse(StrictModeGuard.canChangeTimeLimit(armed = true, currentMillis = 10 * MINUTE, nextMillis = 20 * MINUTE))
        assertTrue(StrictModeGuard.canChangeTimeLimit(armed = true, currentMillis = 10 * MINUTE, nextMillis = 5 * MINUTE))
        assertTrue(StrictModeGuard.canChangeTimeLimit(armed = true, currentMillis = 10 * MINUTE, nextMillis = 10 * MINUTE))
    }

    @Test
    fun armed_intervalConfigRefusesMoreAllowanceOrShorterWindow() {
        // More watch time per window.
        assertFalse(
            StrictModeGuard.canChangeIntervalConfig(
                armed = true,
                currentAllowanceMillis = 5 * MINUTE,
                currentIntervalMillis = 60 * MINUTE,
                nextAllowanceMillis = 10 * MINUTE,
                nextIntervalMillis = 60 * MINUTE,
            ),
        )
        // Same allowance, but handed out twice as often.
        assertFalse(
            StrictModeGuard.canChangeIntervalConfig(
                armed = true,
                currentAllowanceMillis = 5 * MINUTE,
                currentIntervalMillis = 60 * MINUTE,
                nextAllowanceMillis = 5 * MINUTE,
                nextIntervalMillis = 30 * MINUTE,
            ),
        )
    }

    @Test
    fun armed_intervalConfigAllowsTightening() {
        assertTrue(
            StrictModeGuard.canChangeIntervalConfig(
                armed = true,
                currentAllowanceMillis = 5 * MINUTE,
                currentIntervalMillis = 60 * MINUTE,
                nextAllowanceMillis = 3 * MINUTE,
                nextIntervalMillis = 120 * MINUTE,
            ),
        )
        assertTrue(
            StrictModeGuard.canChangeIntervalConfig(
                armed = true,
                currentAllowanceMillis = 5 * MINUTE,
                currentIntervalMillis = 60 * MINUTE,
                nextAllowanceMillis = 5 * MINUTE,
                nextIntervalMillis = 60 * MINUTE,
            ),
        )
    }

    @Test
    fun armed_pausingIsRefusedButCancellingIsNot() {
        assertFalse(StrictModeGuard.canPause(armed = true, shouldPause = true))
        assertTrue(StrictModeGuard.canPause(armed = true, shouldPause = false))
    }

    @Test
    fun armed_togglesCanOnlyMoveTowardsMoreProtection() {
        assertFalse(StrictModeGuard.canChangeExceptReelsSentByDm(armed = true, next = true))
        assertTrue(StrictModeGuard.canChangeExceptReelsSentByDm(armed = true, next = false))

        assertFalse(StrictModeGuard.canChangeInstagramFeedBlocking(armed = true, next = false))
        assertTrue(StrictModeGuard.canChangeInstagramFeedBlocking(armed = true, next = true))
    }

    @Test
    fun armed_minimalModeCanOnlyBeSwitchedOn() {
        assertFalse(StrictModeGuard.canChangeMinimalModeEnabled(armed = true, next = false))
        assertTrue(StrictModeGuard.canChangeMinimalModeEnabled(armed = true, next = true))
        assertTrue(StrictModeGuard.canChangeMinimalModeEnabled(armed = false, next = false))
    }

    @Test
    fun armed_allowingOneMoreAppIsRefusedButRemovingIsNot() {
        assertFalse(StrictModeGuard.canAddAllowedApp(armed = true))
        assertTrue(StrictModeGuard.canAddAllowedApp(armed = false))
    }

    @Test
    fun armed_minimalModeScheduleMayGrowButNotShrink() {
        val current = listOf(MinimalModeWindow(startMinuteOfDay = 22 * 60, endMinuteOfDay = 8 * 60))
        val wider = listOf(MinimalModeWindow(startMinuteOfDay = 21 * 60, endMinuteOfDay = 9 * 60))
        val narrower = listOf(MinimalModeWindow(startMinuteOfDay = 23 * 60, endMinuteOfDay = 7 * 60))

        assertTrue(StrictModeGuard.canChangeMinimalModeSchedule(armed = true, current = current, next = wider))
        assertFalse(StrictModeGuard.canChangeMinimalModeSchedule(armed = true, current = current, next = narrower))
        assertFalse(StrictModeGuard.canChangeMinimalModeSchedule(armed = true, current = current, next = emptyList()))
        assertTrue(StrictModeGuard.canChangeMinimalModeSchedule(armed = false, current = current, next = emptyList()))
    }

    @Test
    fun strictnessRank_ordersOptionsFromStrongestToWeakest() {
        val ranked = BlockOption.entries.sortedByDescending(StrictModeGuard::strictnessRank)
        assertTrue(ranked.first() == BlockOption.BlockAll)
        assertTrue(ranked.last() == BlockOption.NothingSelected)
    }
}
