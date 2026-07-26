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

import com.scrolless.app.core.model.PartnerLink
import kotlinx.coroutines.flow.Flow

sealed class GrantResult {
    /** Code accepted; +15 minutes applied to the current window. */
    data class Granted(val partnerName: String) : GrantResult()

    /** Wrong code; the active challenge survives until attempts run out. */
    data class InvalidCode(val attemptsRemaining: Int) : GrantResult()

    data object NoActiveChallenge : GrantResult()

    data object ChallengeExpired : GrantResult()

    /** Too many wrong codes — the challenge was invalidated, request a new one. */
    data object TooManyAttempts : GrantResult()

    data object NoPartnersPaired : GrantResult()
}

/**
 * Orchestrates partner pairing and the challenge/grant-code lifecycle.
 */
interface PartnerQuotaManager {

    /** People who can grant THIS device extra time. */
    fun getPartners(): Flow<List<PartnerLink>>

    /** People this device can generate grant codes for. */
    fun getWards(): Flow<List<PartnerLink>>

    /** Creates (and persists) a fresh challenge, invalidating any previous one. */
    suspend fun createChallenge(): String

    suspend fun getActiveChallenge(): String?

    /** Verifies a typed grant code against all paired partners. */
    suspend fun submitGrantCode(input: String): GrantResult

    /**
     * Pairs a partner from the secret they displayed.
     *
     * @throws IllegalArgumentException when the typed secret is not a valid pairing secret.
     */
    suspend fun pairPartner(name: String, typedSecret: String)

    /**
     * Creates an identity for a person this device will grant time to and returns the
     * grouped pairing secret. It is shown exactly once — the secret is not recoverable.
     */
    suspend fun createWardIdentity(name: String): String

    /** Computes the grant code for a ward's challenge, or null when the key is gone. */
    suspend fun computeCodeForChallenge(ward: PartnerLink, challenge: String): String?

    /** Removes the pairing and destroys its key material. */
    suspend fun revokeLink(link: PartnerLink)
}
