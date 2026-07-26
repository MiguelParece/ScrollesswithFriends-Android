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
package com.scrolless.app.core.partner

import com.scrolless.app.core.blocking.handler.PartnerQuotaBlockHandler
import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.repository.RedeemedGiftStore
import com.scrolless.app.core.repository.UserSettingsStore
import java.security.SecureRandom
import kotlinx.coroutines.flow.first
import timber.log.Timber

class PartnerQuotaManagerImpl(
    private val userSettingsStore: UserSettingsStore,
    private val redeemedGiftStore: RedeemedGiftStore,
    private val timeProvider: TimeProvider,
    private val secureRandom: SecureRandom = SecureRandom(),
) : PartnerQuotaManager {

    override suspend fun createGiftCode(): String = GiftCodeCrypto.createGiftCode(secureRandom, timeProvider.currentTimeInMillis())

    override suspend fun redeemGiftCode(input: String): RedeemResult {
        val parsed = GiftCodeCrypto.parse(input)
        if (parsed !is GiftCodeCrypto.ParseResult.Valid) return RedeemResult.Invalid

        val now = timeProvider.currentTimeInMillis()
        val age = now - parsed.issuedAtMillis
        if (age > GIFT_TTL_MILLIS || age < -FUTURE_SKEW_MILLIS) {
            Timber.d("Gift code outside validity window (age=%d ms)", age)
            return RedeemResult.Expired
        }

        if (!redeemedGiftStore.tryMarkRedeemed(parsed.nonce, now)) {
            Timber.d("Gift code already redeemed on this device")
            return RedeemResult.AlreadyRedeemed
        }

        applyGrant()
        Timber.i("Gift code redeemed: +%d min", GiftCodeCrypto.GIFT_MINUTES)
        return RedeemResult.Granted
    }

    /**
     * Adds the gift to the window the clock currently points at. When the stored quota
     * row still belongs to an older window, it is rolled forward first so the grant
     * cannot be wiped by the handler's next rollover.
     */
    private suspend fun applyGrant() {
        val storedKey = userSettingsStore.getPartnerQuotaWindowKey().first()
        val currentKey = PartnerQuotaBlockHandler.windowKeyFor(timeProvider.localDateTimeNow())
        if (storedKey == currentKey) {
            userSettingsStore.addPartnerQuotaGrant(GRANT_MILLIS)
        } else {
            userSettingsStore.updatePartnerQuotaState(
                windowKey = currentKey,
                usedMillis = 0L,
                grantedMillis = GRANT_MILLIS,
                anchorWallMillis = timeProvider.currentTimeInMillis(),
                anchorElapsedMillis = timeProvider.elapsedRealtimeMillis(),
                anchorBootCount = timeProvider.bootCount(),
            )
        }
    }

    companion object {
        const val GRANT_MILLIS = GiftCodeCrypto.GIFT_MINUTES * 60_000L
        const val GIFT_TTL_MILLIS = 24 * 3_600_000L
        const val FUTURE_SKEW_MILLIS = 3_600_000L
    }
}
