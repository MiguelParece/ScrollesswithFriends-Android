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
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.core.model.PartnerRole
import com.scrolless.app.core.repository.PartnerRepository
import com.scrolless.app.core.repository.UserSettingsStore
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber

class PartnerQuotaManagerImpl(
    private val partnerRepository: PartnerRepository,
    private val grantCodeSigner: GrantCodeSigner,
    private val userSettingsStore: UserSettingsStore,
    private val timeProvider: TimeProvider,
    private val secureRandom: SecureRandom = SecureRandom(),
) : PartnerQuotaManager {

    override fun getPartners(): Flow<List<PartnerLink>> = partnerRepository.getByRole(PartnerRole.PARTNER)

    override fun getWards(): Flow<List<PartnerLink>> = partnerRepository.getByRole(PartnerRole.WARD)

    override suspend fun createChallenge(): String {
        val challenge = GrantCodeCrypto.generateChallenge(secureRandom)
        userSettingsStore.setActiveChallenge(
            challenge = challenge,
            createdWallMillis = timeProvider.currentTimeInMillis(),
            createdElapsedMillis = timeProvider.elapsedRealtimeMillis(),
            bootCount = timeProvider.bootCount(),
        )
        Timber.d("PartnerQuota: new challenge created")
        return challenge
    }

    override suspend fun getActiveChallenge(): String? = userSettingsStore.getActiveChallenge().first()

    override suspend fun submitGrantCode(input: String): GrantResult {
        val partners = getPartners().first()
        if (partners.isEmpty()) return GrantResult.NoPartnersPaired

        val challenge = userSettingsStore.getActiveChallenge().first() ?: return GrantResult.NoActiveChallenge

        if (isChallengeExpired()) {
            userSettingsStore.clearActiveChallenge()
            return GrantResult.ChallengeExpired
        }

        val attempts = userSettingsStore.getActiveChallengeAttempts().first()
        if (attempts >= MAX_ATTEMPTS) {
            userSettingsStore.clearActiveChallenge()
            return GrantResult.TooManyAttempts
        }

        val typedCode = input.filter(Char::isDigit)
        val match = partners.firstOrNull { partner ->
            val expected = grantCodeSigner.computeCode(partner.keystoreAlias, challenge)
            expected != null && GrantCodeCrypto.constantTimeEquals(expected, typedCode)
        }

        return if (match != null) {
            userSettingsStore.clearActiveChallenge()
            applyGrant()
            Timber.i("PartnerQuota: grant accepted from %s", match.name)
            GrantResult.Granted(match.name)
        } else {
            userSettingsStore.incrementChallengeAttempts()
            val remaining = MAX_ATTEMPTS - (attempts + 1)
            if (remaining <= 0) {
                userSettingsStore.clearActiveChallenge()
                GrantResult.TooManyAttempts
            } else {
                GrantResult.InvalidCode(attemptsRemaining = remaining)
            }
        }
    }

    /**
     * Expiry is measured against wall clock AND (same boot) elapsed realtime, so winding the
     * wall clock back cannot stretch a challenge's lifetime.
     */
    private suspend fun isChallengeExpired(): Boolean {
        val createdWall = userSettingsStore.getActiveChallengeCreatedWall().first()
        val createdElapsed = userSettingsStore.getActiveChallengeCreatedElapsed().first()
        val createdBoot = userSettingsStore.getActiveChallengeBoot().first()

        val wallDelta = timeProvider.currentTimeInMillis() - createdWall
        if (wallDelta < 0L || wallDelta > CHALLENGE_TTL_MILLIS) return true

        val sameBoot = createdBoot >= 0 && createdBoot == timeProvider.bootCount()
        if (sameBoot) {
            val elapsedDelta = timeProvider.elapsedRealtimeMillis() - createdElapsed
            if (elapsedDelta < 0L || elapsedDelta > CHALLENGE_TTL_MILLIS) return true
        }
        return false
    }

    /**
     * Adds the fixed grant to the window the clock currently points at. When the stored
     * quota row still belongs to an older window, it is rolled forward first so the grant
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

    override suspend fun pairPartner(name: String, typedSecret: String) {
        val secret = GrantCodeCrypto.decodeBase32(typedSecret)
        require(secret.size == GrantCodeCrypto.SECRET_LENGTH_BYTES) {
            "Pairing secret must be ${GrantCodeCrypto.SECRET_LENGTH_BYTES} bytes, got ${secret.size}"
        }
        val alias = PARTNER_ALIAS_PREFIX + UUID.randomUUID()
        grantCodeSigner.importSecret(alias, secret)
        partnerRepository.add(name = name.trim(), keystoreAlias = alias, role = PartnerRole.PARTNER)
        Timber.i("PartnerQuota: paired partner %s", name)
    }

    override suspend fun createWardIdentity(name: String): String {
        val secret = GrantCodeCrypto.generateSecret(secureRandom)
        val displayed = GrantCodeCrypto.formatGrouped(GrantCodeCrypto.encodeBase32(secret))
        val alias = WARD_ALIAS_PREFIX + UUID.randomUUID()
        grantCodeSigner.importSecret(alias, secret)
        partnerRepository.add(name = name.trim(), keystoreAlias = alias, role = PartnerRole.WARD)
        Timber.i("PartnerQuota: created ward identity for %s", name)
        return displayed
    }

    override suspend fun computeCodeForChallenge(ward: PartnerLink, challenge: String): String? =
        grantCodeSigner.computeCode(ward.keystoreAlias, challenge)

    override suspend fun revokeLink(link: PartnerLink) {
        grantCodeSigner.deleteKey(link.keystoreAlias)
        partnerRepository.delete(link)
        Timber.i("PartnerQuota: revoked link %s (%s)", link.name, link.role)
    }

    companion object {
        const val GRANT_MILLIS = 15 * 60_000L
        const val MAX_ATTEMPTS = 5
        const val CHALLENGE_TTL_MILLIS = 10 * 60_000L

        private const val PARTNER_ALIAS_PREFIX = "scrolless.partner."
        private const val WARD_ALIAS_PREFIX = "scrolless.ward."
    }
}
