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
package com.scrolless.app.core.domain.grace

import com.scrolless.app.core.blocking.grace.ContentGracePeriodGate
import com.scrolless.app.core.domain.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val BUDGET = 5_000L

class ContentGracePeriodGateTest : BaseTest() {

    private var now = 10_000L
    private val gate = ContentGracePeriodGate(BUDGET) { now }

    private fun advance(millis: Long) {
        now += millis
    }

    @Test
    fun firstVisitGrantsTheFullBudget() {
        gate.onHostAppEntered()

        assertEquals(BUDGET, gate.onContentVisible())
    }

    @Test
    fun dwellShorterThanBudgetReturnsShrinkingRemainder() {
        gate.onHostAppEntered()
        gate.onContentVisible()

        advance(2_000L)

        assertEquals(3_000L, gate.onContentVisible())
    }

    @Test
    fun budgetIsSharedAcrossDwellsInOneVisit() {
        gate.onHostAppEntered()
        gate.onContentVisible()
        advance(2_000L)
        gate.onContentGone() // user went to the inbox

        advance(30_000L) // time away must not be charged
        assertEquals(3_000L, gate.onContentVisible())

        advance(3_000L)
        assertEquals(0L, gate.onContentVisible())
    }

    @Test
    fun spentBudgetStaysSpentForTheRestOfTheVisit() {
        gate.onHostAppEntered()
        gate.onContentVisible()
        advance(BUDGET + 1)

        assertEquals(0L, gate.onContentVisible())
        gate.onContentGone()
        assertEquals(0L, gate.onContentVisible())
    }

    @Test
    fun leavingAndReturningToTheHostAppRefills() {
        gate.onHostAppEntered()
        gate.onContentVisible()
        advance(BUDGET + 1)
        assertEquals(0L, gate.onContentVisible())

        gate.onHostAppExited()
        gate.onHostAppEntered()

        assertEquals(BUDGET, gate.onContentVisible())
    }

    @Test
    fun contentGoneWithoutDwellIsNoOp() {
        gate.onHostAppEntered()

        gate.onContentGone()
        gate.onContentGone()

        assertEquals(BUDGET, gate.onContentVisible())
    }

    @Test
    fun clockGoingBackwardsDoesNotGrantExtraGrace() {
        gate.onHostAppEntered()
        gate.onContentVisible()

        advance(-4_000L)

        assertEquals(BUDGET, gate.onContentVisible())
    }

    @Test
    fun zeroBudgetIsSpentImmediately() {
        val zeroGate = ContentGracePeriodGate(0L) { now }
        zeroGate.onHostAppEntered()

        assertEquals(0L, zeroGate.onContentVisible())
    }
}
