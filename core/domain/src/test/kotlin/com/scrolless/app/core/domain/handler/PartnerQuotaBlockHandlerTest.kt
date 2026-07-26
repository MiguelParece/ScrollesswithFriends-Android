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
package com.scrolless.app.core.domain.handler

import com.scrolless.app.core.blocking.handler.PartnerQuotaBlockHandler
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.model.BlockingResult
import com.scrolless.app.core.model.PartnerQuotaState
import com.scrolless.app.core.model.QuotaWindow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val BASELINE = 15 * 60_000L
private const val MINUTE = 60_000L

class PartnerQuotaBlockHandlerTest : BaseTest() {

    /** Wall clock interpreted in UTC so window boundaries are deterministic. */
    private class FakeTimeProvider(startWallMillis: Long) : TimeProvider {
        var wallMillis = startWallMillis
        var elapsedMillis = 100_000L
        var boot = 1

        override fun currentTimeInMillis() = wallMillis
        override fun localDateNow(): LocalDate = localDateTimeNow().toLocalDate()
        override fun localDateTimeNow(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(wallMillis), ZoneOffset.UTC)

        override fun elapsedRealtimeMillis() = elapsedMillis
        override fun bootCount() = boot

        /** Both clocks tick together — the honest passage of time. */
        fun advance(millis: Long) {
            wallMillis += millis
            elapsedMillis += millis
        }

        /** Only the wall clock moves — the user adjusting the clock. */
        fun adjustWall(deltaMillis: Long) {
            wallMillis += deltaMillis
        }

        fun reboot() {
            boot += 1
            elapsedMillis = 5_000L
        }
    }

    private val stateChanges = mutableListOf<PartnerQuotaState>()

    private fun wallAt(dateTime: String): Long = LocalDateTime.parse(dateTime).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun createHandler(
        time: FakeTimeProvider,
        initialState: PartnerQuotaState = PartnerQuotaState.EMPTY,
    ): PartnerQuotaBlockHandler {
        stateChanges.clear()
        return PartnerQuotaBlockHandler(
            initialState = initialState,
            onStateChanged = { stateChanges.add(it) },
            timeProvider = time,
        )
    }

    @Test
    fun windowFor_boundaries() {
        assertEquals(QuotaWindow.NIGHT, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T04:59")))
        assertEquals(QuotaWindow.MORNING, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T05:00")))
        assertEquals(QuotaWindow.MORNING, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T11:59")))
        assertEquals(QuotaWindow.AFTERNOON, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T12:00")))
        assertEquals(QuotaWindow.AFTERNOON, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T17:59")))
        assertEquals(QuotaWindow.NIGHT, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T18:00")))
        assertEquals(QuotaWindow.NIGHT, PartnerQuotaBlockHandler.windowFor(LocalDateTime.parse("2026-07-26T23:59")))
    }

    @Test
    fun windowKeyFor_nightSpansMidnight() {
        assertEquals(
            "2026-07-26|NIGHT",
            PartnerQuotaBlockHandler.windowKeyFor(LocalDateTime.parse("2026-07-26T23:30")),
        )
        assertEquals(
            "2026-07-26|NIGHT",
            PartnerQuotaBlockHandler.windowKeyFor(LocalDateTime.parse("2026-07-27T00:30")),
        )
        assertEquals(
            "2026-07-26|NIGHT",
            PartnerQuotaBlockHandler.windowKeyFor(LocalDateTime.parse("2026-07-27T04:59")),
        )
        assertEquals(
            "2026-07-27|MORNING",
            PartnerQuotaBlockHandler.windowKeyFor(LocalDateTime.parse("2026-07-27T05:00")),
        )
    }

    @Test
    fun onEnterContent_underBaseline_doesNotBlock() {
        val time = FakeTimeProvider(wallAt("2026-07-26T08:00"))
        val handler = createHandler(time)

        assertFalse(handler.onEnterContent(0L))
    }

    @Test
    fun onEnterContent_atBaseline_blocks() {
        val time = FakeTimeProvider(wallAt("2026-07-26T08:00"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|MORNING",
                usedMillis = BASELINE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        assertTrue(handler.onEnterContent(0L))
    }

    @Test
    fun onPeriodicCheck_exceedingBaseline_blocksAndClamps() {
        val time = FakeTimeProvider(wallAt("2026-07-26T08:00"))
        val handler = createHandler(time)
        handler.onEnterContent(0L)

        time.advance(16 * MINUTE)
        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 16 * MINUTE)

        assertTrue(result is BlockingResult.BlockNow)
        assertEquals(BASELINE, stateChanges.last().usedMillis)
    }

    @Test
    fun onPeriodicCheck_underLimit_checksLaterWithRemaining() {
        val time = FakeTimeProvider(wallAt("2026-07-26T08:00"))
        val handler = createHandler(time)
        handler.onEnterContent(0L)

        time.advance(5 * MINUTE)
        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 5 * MINUTE)

        assertTrue(result is BlockingResult.CheckLater)
        assertEquals(10 * MINUTE, (result as BlockingResult.CheckLater).delayMillis)
    }

    @Test
    fun onExitContent_accumulatesUsageAcrossSessions() {
        val time = FakeTimeProvider(wallAt("2026-07-26T08:00"))
        val handler = createHandler(time)

        handler.onEnterContent(0L)
        time.advance(4 * MINUTE)
        handler.onExitContent(4 * MINUTE)

        assertEquals(4 * MINUTE, stateChanges.last().usedMillis)

        time.advance(10 * MINUTE)
        handler.onEnterContent(0L)
        time.advance(3 * MINUTE)
        handler.onExitContent(3 * MINUTE)

        assertEquals(7 * MINUTE, stateChanges.last().usedMillis)
    }

    @Test
    fun grant_raisesLimit() {
        val time = FakeTimeProvider(wallAt("2026-07-26T08:00"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|MORNING",
                usedMillis = BASELINE,
                grantedMillis = 15 * MINUTE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        assertFalse(handler.onEnterContent(0L))

        time.advance(16 * MINUTE)
        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 16 * MINUTE)
        assertTrue(result is BlockingResult.BlockNow)
        assertEquals(BASELINE + 15 * MINUTE, stateChanges.last().usedMillis)
    }

    @Test
    fun trustedRollover_resetsUsageAndGrants() {
        val time = FakeTimeProvider(wallAt("2026-07-26T11:50"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|MORNING",
                usedMillis = BASELINE,
                grantedMillis = 15 * MINUTE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        // Honest passage of time into the afternoon window.
        time.advance(20 * MINUTE)
        assertFalse(handler.onEnterContent(0L))

        val rolled = stateChanges.last()
        assertEquals("2026-07-26|AFTERNOON", rolled.windowKey)
        assertEquals(0L, rolled.usedMillis)
        assertEquals(0L, rolled.grantedMillis)
    }

    @Test
    fun nightWindow_noResetAcrossMidnight() {
        val time = FakeTimeProvider(wallAt("2026-07-26T23:50"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|NIGHT",
                usedMillis = 10 * MINUTE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        time.advance(40 * MINUTE) // 00:30 next day, still the same night window
        handler.onEnterContent(0L)

        assertTrue(stateChanges.isEmpty())
    }

    @Test
    fun midSessionRollover_excludesPreBoundaryUsageFromNewWindow() {
        val time = FakeTimeProvider(wallAt("2026-07-26T11:58"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|MORNING",
                usedMillis = 5 * MINUTE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )
        handler.onEnterContent(0L)

        // Session crosses the 12:00 boundary after 3 minutes.
        time.advance(3 * MINUTE)
        val result = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 3 * MINUTE)
        assertTrue(result is BlockingResult.CheckLater)
        assertEquals(BASELINE, (result as BlockingResult.CheckLater).delayMillis)

        // One more minute inside the new window counts against it.
        time.advance(1 * MINUTE)
        val next = handler.onPeriodicCheck(currentDailyUsage = 0L, elapsedTime = 4 * MINUTE)
        assertEquals(BASELINE - 1 * MINUTE, (next as BlockingResult.CheckLater).delayMillis)
    }

    @Test
    fun backwardClockJump_carriesCountersInsteadOfRefilling() {
        val time = FakeTimeProvider(wallAt("2026-07-26T13:00"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|AFTERNOON",
                usedMillis = BASELINE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        // User drags the clock back into the morning window hoping for fresh quota.
        time.adjustWall(-3 * 3_600_000L)
        assertTrue(handler.onEnterContent(0L))

        val carried = stateChanges.last()
        assertEquals("2026-07-26|MORNING", carried.windowKey)
        assertEquals(BASELINE, carried.usedMillis)
    }

    @Test
    fun forwardClockJump_sameBoot_carriesCounters() {
        val time = FakeTimeProvider(wallAt("2026-07-26T10:00"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|MORNING",
                usedMillis = BASELINE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        // Clock pushed 5 hours ahead without matching elapsed time.
        time.adjustWall(5 * 3_600_000L)
        assertTrue(handler.onEnterContent(0L))

        val carried = stateChanges.last()
        assertEquals("2026-07-26|AFTERNOON", carried.windowKey)
        assertEquals(BASELINE, carried.usedMillis)
    }

    @Test
    fun rebootWithForwardWall_allowsRollover() {
        val time = FakeTimeProvider(wallAt("2026-07-26T11:00"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|MORNING",
                usedMillis = BASELINE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        // Device rebooted and comes back in the afternoon: cannot verify, accept forward.
        time.adjustWall(2 * 3_600_000L)
        time.reboot()
        assertFalse(handler.onEnterContent(0L))

        val rolled = stateChanges.last()
        assertEquals("2026-07-26|AFTERNOON", rolled.windowKey)
        assertEquals(0L, rolled.usedMillis)
    }

    @Test
    fun rebootWithBackwardWall_carriesCounters() {
        val time = FakeTimeProvider(wallAt("2026-07-26T13:00"))
        val handler = createHandler(
            time,
            initialState = PartnerQuotaState.EMPTY.copy(
                windowKey = "2026-07-26|AFTERNOON",
                usedMillis = BASELINE,
                anchorWallMillis = time.wallMillis,
                anchorElapsedMillis = time.elapsedMillis,
                anchorBootCount = 1,
            ),
        )

        time.adjustWall(-2 * 3_600_000L)
        time.reboot()
        assertTrue(handler.onEnterContent(0L))

        val carried = stateChanges.last()
        assertEquals("2026-07-26|MORNING", carried.windowKey)
        assertEquals(BASELINE, carried.usedMillis)
    }
}
