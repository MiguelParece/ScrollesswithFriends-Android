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
package com.scrolless.app.core.domain.time

import com.scrolless.app.core.blocking.time.TrustedWallClock
import com.scrolless.app.core.domain.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val ANCHOR_WALL = 1_700_000_000_000L
private const val ANCHOR_ELAPSED = 500_000L
private const val ANCHOR_BOOT = 7

private const val ONE_HOUR = 3_600_000L
private const val TEN_MINUTES = 600_000L

class TrustedWallClockTest : BaseTest() {

    private fun nowMillis(
        wallMillis: Long,
        elapsedMillis: Long,
        bootCount: Int = ANCHOR_BOOT,
        anchorWallMillis: Long = ANCHOR_WALL,
        anchorBootCount: Int = ANCHOR_BOOT,
    ) = TrustedWallClock.nowMillis(
        anchorWallMillis = anchorWallMillis,
        anchorElapsedMillis = ANCHOR_ELAPSED,
        anchorBootCount = anchorBootCount,
        wallMillis = wallMillis,
        elapsedMillis = elapsedMillis,
        bootCount = bootCount,
    )

    @Test
    fun withoutAnAnchorTheSystemClockIsTakenOnTrust() {
        assertEquals(
            ANCHOR_WALL,
            nowMillis(wallMillis = ANCHOR_WALL, elapsedMillis = ANCHOR_ELAPSED, anchorWallMillis = 0L),
        )
    }

    @Test
    fun inBootTimeAdvancesWithElapsedRealtime() {
        assertEquals(
            ANCHOR_WALL + TEN_MINUTES,
            nowMillis(
                wallMillis = ANCHOR_WALL + TEN_MINUTES,
                elapsedMillis = ANCHOR_ELAPSED + TEN_MINUTES,
            ),
        )
    }

    /** The whole point: winding the clock forward must not move the schedule. */
    @Test
    fun inBootAForwardClockJumpIsIgnored() {
        assertEquals(
            ANCHOR_WALL + TEN_MINUTES,
            nowMillis(
                wallMillis = ANCHOR_WALL + TEN_MINUTES + ONE_HOUR,
                elapsedMillis = ANCHOR_ELAPSED + TEN_MINUTES,
            ),
        )
    }

    @Test
    fun inBootABackwardClockJumpIsIgnored() {
        assertEquals(
            ANCHOR_WALL + TEN_MINUTES,
            nowMillis(
                wallMillis = ANCHOR_WALL - ONE_HOUR,
                elapsedMillis = ANCHOR_ELAPSED + TEN_MINUTES,
            ),
        )
    }

    @Test
    fun acrossABootTheSystemClockIsAcceptedWhenItMovedForward() {
        assertEquals(
            ANCHOR_WALL + ONE_HOUR,
            nowMillis(
                wallMillis = ANCHOR_WALL + ONE_HOUR,
                elapsedMillis = 1_000L,
                bootCount = ANCHOR_BOOT + 1,
            ),
        )
    }

    /** Rebooting with the clock wound back is the cheap escape; the anchor is the floor. */
    @Test
    fun acrossABootTheClockNeverRewindsPastTheAnchor() {
        assertEquals(
            ANCHOR_WALL,
            nowMillis(
                wallMillis = ANCHOR_WALL - ONE_HOUR,
                elapsedMillis = 1_000L,
                bootCount = ANCHOR_BOOT + 1,
            ),
        )
    }

    /** Below API 24 there is no BOOT_COUNT, so a non-decreasing elapsed clock stands in. */
    @Test
    fun withoutBootCountsARisingElapsedClockCountsAsTheSameBoot() {
        assertEquals(
            ANCHOR_WALL + TEN_MINUTES,
            nowMillis(
                wallMillis = ANCHOR_WALL + ONE_HOUR,
                elapsedMillis = ANCHOR_ELAPSED + TEN_MINUTES,
                bootCount = -1,
                anchorBootCount = -1,
            ),
        )
    }

    @Test
    fun withoutBootCountsAResetElapsedClockIsTreatedAsAReboot() {
        assertEquals(
            ANCHOR_WALL + ONE_HOUR,
            nowMillis(
                wallMillis = ANCHOR_WALL + ONE_HOUR,
                elapsedMillis = 1_000L,
                bootCount = -1,
                anchorBootCount = -1,
            ),
        )
    }
}
