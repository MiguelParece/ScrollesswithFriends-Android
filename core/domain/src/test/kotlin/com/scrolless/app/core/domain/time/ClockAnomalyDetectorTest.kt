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

import com.scrolless.app.core.blocking.time.ClockAnomalyDetector
import com.scrolless.app.core.blocking.time.ClockAnomalyDetector.Verdict
import com.scrolless.app.core.domain.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockAnomalyDetectorTest : BaseTest() {

    private fun evaluate(
        anchorWall: Long = 1_000_000L,
        anchorElapsed: Long = 50_000L,
        anchorBoot: Int = 3,
        wall: Long,
        elapsed: Long,
        boot: Int = 3,
    ): Verdict = ClockAnomalyDetector.evaluate(
        anchorWallMillis = anchorWall,
        anchorElapsedMillis = anchorElapsed,
        anchorBootCount = anchorBoot,
        wallMillis = wall,
        elapsedMillis = elapsed,
        bootCount = boot,
    )

    @Test
    fun noAnchor_isConsistent() {
        val verdict = evaluate(anchorWall = 0L, wall = 500L, elapsed = 500L)
        assertEquals(Verdict.CONSISTENT, verdict)
    }

    @Test
    fun sameBoot_lockstepAdvance_isConsistent() {
        // Wall and elapsed both advanced by 10 minutes.
        val verdict = evaluate(wall = 1_600_000L, elapsed = 650_000L)
        assertEquals(Verdict.CONSISTENT, verdict)
    }

    @Test
    fun sameBoot_driftWithinTolerance_isConsistent() {
        val verdict = evaluate(wall = 1_600_000L + 60_000L, elapsed = 650_000L)
        assertEquals(Verdict.CONSISTENT, verdict)
    }

    @Test
    fun sameBoot_forwardJumpBeyondTolerance_isSuspicious() {
        // Wall jumped 7h while elapsed advanced 1 minute.
        val verdict = evaluate(wall = 1_000_000L + 7 * 3_600_000L, elapsed = 51_000L)
        assertEquals(Verdict.SUSPICIOUS, verdict)
    }

    @Test
    fun sameBoot_backwardJump_isSuspicious() {
        // Wall moved 2h back while elapsed advanced.
        val verdict = evaluate(wall = 1_000_000L - 2 * 3_600_000L, elapsed = 60_000L)
        assertEquals(Verdict.SUSPICIOUS, verdict)
    }

    @Test
    fun reboot_forwardWall_isUnverifiedForward() {
        val verdict = evaluate(wall = 5_000_000L, elapsed = 10_000L, boot = 4)
        assertEquals(Verdict.UNVERIFIED_FORWARD, verdict)
    }

    @Test
    fun reboot_backwardWall_isSuspicious() {
        val verdict = evaluate(wall = 500_000L, elapsed = 10_000L, boot = 4)
        assertEquals(Verdict.SUSPICIOUS, verdict)
    }

    @Test
    fun bootCountUnavailable_elapsedReset_forwardWall_isUnverifiedForward() {
        val verdict = evaluate(anchorBoot = -1, wall = 5_000_000L, elapsed = 10_000L, boot = -1)
        assertEquals(Verdict.UNVERIFIED_FORWARD, verdict)
    }

    @Test
    fun bootCountUnavailable_elapsedAdvancing_lockstep_isConsistent() {
        val verdict = evaluate(anchorBoot = -1, wall = 1_600_000L, elapsed = 650_000L, boot = -1)
        assertEquals(Verdict.CONSISTENT, verdict)
    }

    @Test
    fun bootCountUnavailable_elapsedAdvancing_backwardWall_isSuspicious() {
        val verdict = evaluate(anchorBoot = -1, wall = 400_000L, elapsed = 650_000L, boot = -1)
        assertEquals(Verdict.SUSPICIOUS, verdict)
    }
}
