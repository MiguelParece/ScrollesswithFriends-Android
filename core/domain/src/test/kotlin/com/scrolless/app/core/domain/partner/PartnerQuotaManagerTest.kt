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
package com.scrolless.app.core.domain.partner

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.partner.PartnerQuotaManagerImpl
import com.scrolless.app.core.partner.RedeemResult
import com.scrolless.app.core.repository.RedeemedGiftStore
import com.scrolless.app.core.repository.UserSettingsStore
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val HOUR = 3_600_000L

class PartnerQuotaManagerTest : BaseTest() {

    private class FakeTime : TimeProvider {
        var wallMillis = LocalDateTime.parse("2026-07-26T10:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        var elapsedMillis = 500_000L

        override fun currentTimeInMillis() = wallMillis
        override fun localDateNow(): LocalDate = localDateTimeNow().toLocalDate()
        override fun localDateTimeNow(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(wallMillis), ZoneOffset.UTC)

        override fun elapsedRealtimeMillis() = elapsedMillis
        override fun bootCount() = 1
    }

    private class FakeRedeemedGiftStore : RedeemedGiftStore {
        val redeemed = mutableSetOf<String>()

        override suspend fun tryMarkRedeemed(nonce: String, redeemedAtMillis: Long): Boolean = redeemed.add(nonce)
    }

    private val time = FakeTime()
    private val redeemedStore = FakeRedeemedGiftStore()
    private val settingsStore = mockk<UserSettingsStore>(relaxed = true)

    private lateinit var manager: PartnerQuotaManagerImpl

    @Before
    fun setUpManager() {
        every { settingsStore.getPartnerQuotaWindowKey() } returns flowOf("2026-07-26|MORNING")
        manager = PartnerQuotaManagerImpl(
            userSettingsStore = settingsStore,
            redeemedGiftStore = redeemedStore,
            timeProvider = time,
        )
    }

    @Test
    fun redeem_freshCode_grants() = runTest {
        val code = manager.createGiftCode()

        assertEquals(RedeemResult.Granted, manager.redeemGiftCode(code))
        coVerify { settingsStore.addPartnerQuotaGrant(PartnerQuotaManagerImpl.GRANT_MILLIS) }
    }

    @Test
    fun redeem_insideWhatsAppMessage_grants() = runTest {
        val code = manager.createGiftCode()

        val result = manager.redeemGiftCode("🎁 here you go!! $code use it well")

        assertEquals(RedeemResult.Granted, result)
    }

    @Test
    fun redeem_secondTime_reportsAlreadyRedeemed() = runTest {
        val code = manager.createGiftCode()

        assertEquals(RedeemResult.Granted, manager.redeemGiftCode(code))
        assertEquals(RedeemResult.AlreadyRedeemed, manager.redeemGiftCode(code))
    }

    @Test
    fun redeem_garbage_reportsInvalid() = runTest {
        assertEquals(RedeemResult.Invalid, manager.redeemGiftCode("nothing here"))
    }

    @Test
    fun redeem_dayOldCode_reportsExpired() = runTest {
        val code = manager.createGiftCode()

        time.wallMillis += 25 * HOUR

        assertEquals(RedeemResult.Expired, manager.redeemGiftCode(code))
    }

    @Test
    fun redeem_farFutureCode_reportsExpired() = runTest {
        time.wallMillis += 3 * HOUR
        val futureCode = manager.createGiftCode()
        time.wallMillis -= 3 * HOUR

        assertEquals(RedeemResult.Expired, manager.redeemGiftCode(futureCode))
    }

    @Test
    fun redeem_afterWindowChange_rollsQuotaRowForward() = runTest {
        every { settingsStore.getPartnerQuotaWindowKey() } returns flowOf("2026-07-25|NIGHT")
        val code = manager.createGiftCode()

        assertEquals(RedeemResult.Granted, manager.redeemGiftCode(code))

        coVerify {
            settingsStore.updatePartnerQuotaState(
                windowKey = "2026-07-26|MORNING",
                usedMillis = 0L,
                grantedMillis = PartnerQuotaManagerImpl.GRANT_MILLIS,
                anchorWallMillis = any(),
                anchorElapsedMillis = any(),
                anchorBootCount = any(),
            )
        }
    }
}
