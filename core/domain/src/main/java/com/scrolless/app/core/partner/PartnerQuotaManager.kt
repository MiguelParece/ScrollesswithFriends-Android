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

sealed class RedeemResult {
    /** Code accepted; +15 minutes applied to the current window. */
    data object Granted : RedeemResult()

    /** Already redeemed on this device — gift codes work exactly once. */
    data object AlreadyRedeemed : RedeemResult()

    /** Older than the validity window (or absurdly in the future). */
    data object Expired : RedeemResult()

    /** No valid gift code found in the pasted text. */
    data object Invalid : RedeemResult()
}

/**
 * Creates and redeems transferable "+15 minutes" gift codes for the Partner Quota
 * block option. Codes travel between people over any channel; this device only
 * generates and validates them locally.
 */
interface PartnerQuotaManager {

    /** Generates a shareable gift code (grouped base32). */
    suspend fun createGiftCode(): String

    /** Extracts a gift code from pasted text (or a deep link) and redeems it. */
    suspend fun redeemGiftCode(input: String): RedeemResult
}
