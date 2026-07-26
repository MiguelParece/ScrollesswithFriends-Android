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
import com.scrolless.app.core.domain.utils.TestSchedulerTimeProvider
import com.scrolless.app.core.model.StrictArmResult
import com.scrolless.app.core.model.StrictModeState
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.core.strict.StrictModeManager
import com.scrolless.app.core.strict.StrictModeManagerImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val HOUR = 3_600_000L
private const val DAY = 24 * HOUR

@OptIn(ExperimentalCoroutinesApi::class)
class StrictModeManagerTest : BaseTest() {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var time: TestSchedulerTimeProvider
    private lateinit var manager: StrictModeManagerImpl

    private var stored = StrictModeState.EMPTY
    private val store = mockk<UserSettingsStore>(relaxed = true)

    @Before
    fun setUpManager() {
        time = TestSchedulerTimeProvider(testDispatcher.scheduler)
        every { store.getStrictUntil() } answers { flowOf(stored.untilAtMillis) }
        every { store.getStrictAnchorWall() } answers { flowOf(stored.anchorWallMillis) }
        every { store.getStrictAnchorElapsed() } answers { flowOf(stored.anchorElapsedMillis) }
        every { store.getStrictAnchorBoot() } answers { flowOf(stored.anchorBootCount) }
        coEvery { store.updateStrictModeState(any(), any(), any(), any()) } answers {
            stored = StrictModeState(
                untilAtMillis = arg(0),
                anchorWallMillis = arg(1),
                anchorElapsedMillis = arg(2),
                anchorBootCount = arg(3),
            )
        }
        manager = StrictModeManagerImpl(userSettingsStore = store, timeProvider = time)
    }

    private fun advance(millis: Long) = testDispatcher.scheduler.advanceTimeBy(millis)

    @Test
    fun arm_validDuration_persistsEndAndAnchors() = runTest(testDispatcher) {
        advance(HOUR)

        val result = manager.arm(DAY)

        assertTrue(result is StrictArmResult.Armed)
        assertEquals(time.currentTimeInMillis() + DAY, stored.untilAtMillis)
        assertEquals(time.currentTimeInMillis(), stored.anchorWallMillis)
        assertEquals(time.elapsedRealtimeMillis(), stored.anchorElapsedMillis)
        assertEquals(time.bootCount(), stored.anchorBootCount)
    }

    @Test
    fun arm_overCap_clampedTo30Days() = runTest(testDispatcher) {
        advance(HOUR)

        manager.arm(90 * DAY)

        assertEquals(
            time.currentTimeInMillis() + StrictModeManager.MAX_DURATION_MILLIS,
            stored.untilAtMillis,
        )
    }

    @Test
    fun arm_nonPositiveDuration_rejected() = runTest(testDispatcher) {
        assertEquals(StrictArmResult.RejectedInvalidDuration, manager.arm(0L))
        assertEquals(StrictArmResult.RejectedInvalidDuration, manager.arm(-5L))
    }

    @Test
    fun arm_whileArmed_longerThanRemaining_extends() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(DAY)

        advance(HOUR) // 23h remaining
        val result = manager.arm(3 * DAY)

        assertTrue(result is StrictArmResult.Armed)
        assertEquals(time.currentTimeInMillis() + 3 * DAY, stored.untilAtMillis)
    }

    @Test
    fun arm_whileArmed_shorterThanRemaining_rejected() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(3 * DAY)

        advance(HOUR)
        val result = manager.arm(DAY)

        assertEquals(StrictArmResult.RejectedWouldShorten, result)
    }

    @Test
    fun arm_extendAfterWallRollback_usesTrustedRemaining() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(3 * DAY)

        // User rolls the wall clock back 2 days; true remaining is still ~3 days.
        time.wallClockOffsetMillis = -2 * DAY
        val result = manager.arm(2 * DAY)

        assertEquals(StrictArmResult.RejectedWouldShorten, result)
    }

    @Test
    fun isArmed_inBoot_wallForward40Days_staysArmed() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(7 * DAY)

        time.wallClockOffsetMillis = 40 * DAY

        assertTrue(manager.isArmed(stored))
    }

    @Test
    fun isArmed_inBoot_wallBackward_staysArmed_remainingUnchanged() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(DAY)
        advance(HOUR)

        val remainingBefore = manager.remainingMillis(stored)
        time.wallClockOffsetMillis = -10 * DAY

        assertTrue(manager.isArmed(stored))
        assertEquals(remainingBefore, manager.remainingMillis(stored))
    }

    @Test
    fun isArmed_naturalElapse_disarms() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(DAY)

        advance(DAY + 1)

        assertFalse(manager.isArmed(stored))
        assertEquals(0L, manager.remainingMillis(stored))
    }

    @Test
    fun isArmed_reboot_beforeEnd_staysArmed() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(7 * DAY)

        advance(DAY)
        time.simulateReboot()

        assertTrue(manager.isArmed(stored))
    }

    @Test
    fun isArmed_reboot_wallPastEnd_disarms() = runTest(testDispatcher) {
        // Documents the accepted bypass: clock pushed forward across a reboot is
        // unverifiable (UNVERIFIED_FORWARD) and therefore accepted.
        advance(HOUR)
        manager.arm(7 * DAY)

        advance(DAY)
        time.simulateReboot()
        time.wallClockOffsetMillis = 10 * DAY

        assertFalse(manager.isArmed(stored))
    }

    @Test
    fun isArmed_reboot_wallBeforeAnchor_suspicious_staysArmed() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(7 * DAY)

        advance(DAY)
        time.simulateReboot()
        time.wallClockOffsetMillis = -3 * DAY

        assertTrue(manager.isArmed(stored))
    }

    @Test
    fun remainingMillis_countsDownWithElapsed() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(DAY)

        advance(6 * HOUR)

        assertEquals(18 * HOUR, manager.remainingMillis(stored))
    }

    @Test
    fun reanchor_afterReboot_restoresElapsedEnforcement() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(7 * DAY)

        advance(DAY)
        time.simulateReboot()
        manager.reanchorIfNeeded()

        assertEquals(time.bootCount(), stored.anchorBootCount)
        // Post-reanchor we are back on the same-boot path: a forward wall jump is ignored.
        time.wallClockOffsetMillis = 40 * DAY
        assertTrue(manager.isArmed(stored))
    }

    @Test
    fun reanchor_suspiciousClock_doesNotRewrite() = runTest(testDispatcher) {
        advance(HOUR)
        manager.arm(7 * DAY)
        val anchorsBefore = stored

        advance(DAY)
        time.simulateReboot()
        time.wallClockOffsetMillis = -3 * DAY
        manager.reanchorIfNeeded()

        assertEquals(anchorsBefore, stored)
    }
}
