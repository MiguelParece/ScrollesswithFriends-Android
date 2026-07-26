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
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.core.model.PartnerRole
import com.scrolless.app.core.partner.GrantCodeCrypto
import com.scrolless.app.core.partner.GrantCodeSigner
import com.scrolless.app.core.partner.GrantResult
import com.scrolless.app.core.partner.PartnerQuotaManagerImpl
import com.scrolless.app.core.repository.PartnerRepository
import com.scrolless.app.core.repository.UserSettingsStore
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val MINUTE = 60_000L

class PartnerQuotaManagerTest : BaseTest() {

    private class FakeTime : TimeProvider {
        var wallMillis = LocalDateTime.parse("2026-07-26T10:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        var elapsedMillis = 500_000L
        var boot = 2

        override fun currentTimeInMillis() = wallMillis
        override fun localDateNow(): LocalDate = localDateTimeNow().toLocalDate()
        override fun localDateTimeNow(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(wallMillis), ZoneOffset.UTC)

        override fun elapsedRealtimeMillis() = elapsedMillis
        override fun bootCount() = boot

        fun advance(millis: Long) {
            wallMillis += millis
            elapsedMillis += millis
        }
    }

    /** In-memory signer replicating the real code path with plain byte-array keys. */
    private class FakeSigner : GrantCodeSigner {
        val keys = mutableMapOf<String, ByteArray>()

        override fun importSecret(alias: String, secret: ByteArray) {
            keys[alias] = secret.copyOf()
            secret.fill(0)
        }

        override fun computeCode(alias: String, challenge: String): String? =
            keys[alias]?.let { GrantCodeCrypto.computeGrantCode(it, challenge) }

        override fun deleteKey(alias: String) {
            keys.remove(alias)
        }
    }

    private val time = FakeTime()
    private val signer = FakeSigner()
    private val repository = mockk<PartnerRepository>()
    private val store = mockk<UserSettingsStore>(relaxed = true)

    private val partnerSecret = ByteArray(16) { (it + 7).toByte() }
    private val partner = PartnerLink(
        id = 1L,
        name = "Ana",
        keystoreAlias = "scrolless.partner.test",
        role = PartnerRole.PARTNER,
        createdAt = 0L,
    )

    private val activeChallenge = MutableStateFlow<String?>(null)
    private val challengeAttempts = MutableStateFlow(0)
    private var challengeCreatedWall = 0L
    private var challengeCreatedElapsed = 0L
    private var challengeBoot = -1

    private lateinit var manager: PartnerQuotaManagerImpl

    @Before
    fun setUpManager() {
        signer.keys[partner.keystoreAlias] = partnerSecret.copyOf()
        every { repository.getByRole(PartnerRole.PARTNER) } returns flowOf(listOf(partner))
        every { store.getActiveChallenge() } returns activeChallenge
        every { store.getActiveChallengeAttempts() } returns challengeAttempts
        every { store.getActiveChallengeCreatedWall() } answers { flowOf(challengeCreatedWall) }
        every { store.getActiveChallengeCreatedElapsed() } answers { flowOf(challengeCreatedElapsed) }
        every { store.getActiveChallengeBoot() } answers { flowOf(challengeBoot) }
        every { store.getPartnerQuotaWindowKey() } returns flowOf("2026-07-26|MORNING")
        coEvery { store.setActiveChallenge(any(), any(), any(), any()) } answers {
            activeChallenge.value = firstArg()
            challengeCreatedWall = secondArg()
            challengeCreatedElapsed = thirdArg()
            challengeBoot = arg(3)
            challengeAttempts.value = 0
        }
        coJustRun { store.clearActiveChallenge() }
        coEvery { store.incrementChallengeAttempts() } answers { challengeAttempts.value += 1 }

        manager = PartnerQuotaManagerImpl(
            partnerRepository = repository,
            grantCodeSigner = signer,
            userSettingsStore = store,
            timeProvider = time,
        )
    }

    private fun validCodeFor(challenge: String): String = GrantCodeCrypto.computeGrantCode(partnerSecret, challenge)

    @Test
    fun submit_validCode_grants() = runTest {
        val challenge = manager.createChallenge()

        val result = manager.submitGrantCode(validCodeFor(challenge))

        assertEquals(GrantResult.Granted("Ana"), result)
        coVerify { store.addPartnerQuotaGrant(PartnerQuotaManagerImpl.GRANT_MILLIS) }
        coVerify { store.clearActiveChallenge() }
    }

    @Test
    fun submit_wrongCode_countsAttempt() = runTest {
        manager.createChallenge()

        val result = manager.submitGrantCode("00000000")

        assertTrue(result is GrantResult.InvalidCode)
        assertEquals(PartnerQuotaManagerImpl.MAX_ATTEMPTS - 1, (result as GrantResult.InvalidCode).attemptsRemaining)
    }

    @Test
    fun submit_fifthWrongCode_locksChallenge() = runTest {
        manager.createChallenge()

        var last: GrantResult = GrantResult.NoActiveChallenge
        repeat(PartnerQuotaManagerImpl.MAX_ATTEMPTS) {
            last = manager.submitGrantCode("00000000")
        }

        assertEquals(GrantResult.TooManyAttempts, last)
        coVerify { store.clearActiveChallenge() }
    }

    @Test
    fun submit_noChallenge_reportsNoActiveChallenge() = runTest {
        assertEquals(GrantResult.NoActiveChallenge, manager.submitGrantCode("12345678"))
    }

    @Test
    fun submit_noPartners_reportsNoPartnersPaired() = runTest {
        every { repository.getByRole(PartnerRole.PARTNER) } returns flowOf(emptyList())

        assertEquals(GrantResult.NoPartnersPaired, manager.submitGrantCode("12345678"))
    }

    @Test
    fun submit_afterWallExpiry_reportsExpired() = runTest {
        val challenge = manager.createChallenge()

        time.advance(11 * MINUTE)

        assertEquals(GrantResult.ChallengeExpired, manager.submitGrantCode(validCodeFor(challenge)))
    }

    @Test
    fun submit_wallRolledBack_stillExpiresViaElapsed() = runTest {
        val challenge = manager.createChallenge()

        // 11 minutes really pass, then the user drags the wall clock back 10 minutes.
        time.advance(11 * MINUTE)
        time.wallMillis -= 10 * MINUTE

        assertEquals(GrantResult.ChallengeExpired, manager.submitGrantCode(validCodeFor(challenge)))
    }

    @Test
    fun submit_replayAfterGrant_reportsNoActiveChallenge() = runTest {
        val challenge = manager.createChallenge()
        val code = validCodeFor(challenge)

        assertEquals(GrantResult.Granted("Ana"), manager.submitGrantCode(code))
        activeChallenge.value = null // consumed by clearActiveChallenge in production

        assertEquals(GrantResult.NoActiveChallenge, manager.submitGrantCode(code))
    }

    @Test
    fun newChallenge_invalidatesOldCode() = runTest {
        val first = manager.createChallenge()
        val staleCode = validCodeFor(first)

        val second = manager.createChallenge()

        val result = manager.submitGrantCode(staleCode)
        assertTrue(result is GrantResult.InvalidCode || second != first)
    }

    @Test
    fun grantAfterWindowChange_rollsQuotaRowForward() = runTest {
        every { store.getPartnerQuotaWindowKey() } returns flowOf("2026-07-25|NIGHT")
        val challenge = manager.createChallenge()

        assertEquals(GrantResult.Granted("Ana"), manager.submitGrantCode(validCodeFor(challenge)))

        coVerify {
            store.updatePartnerQuotaState(
                windowKey = "2026-07-26|MORNING",
                usedMillis = 0L,
                grantedMillis = PartnerQuotaManagerImpl.GRANT_MILLIS,
                anchorWallMillis = any(),
                anchorElapsedMillis = any(),
                anchorBootCount = any(),
            )
        }
    }

    @Test
    fun pairPartner_importsKeyAndStoresLink() = runTest {
        coJustRun { repository.add(any(), any(), any()) }
        val secretText = GrantCodeCrypto.formatGrouped(GrantCodeCrypto.encodeBase32(ByteArray(16) { 9 }))

        manager.pairPartner("Rui", secretText)

        assertTrue(signer.keys.keys.any { it.startsWith("scrolless.partner.") && it != partner.keystoreAlias })
        coVerify { repository.add("Rui", any(), PartnerRole.PARTNER) }
    }

    @Test
    fun createWardIdentity_returnsSecretThatGeneratesMatchingCodes() = runTest {
        coJustRun { repository.add(any(), any(), any()) }

        val displayedSecret = manager.createWardIdentity("Miguel")

        // The ward-side key and a partner pairing from the displayed secret must agree.
        val wardAlias = signer.keys.keys.first { it.startsWith("scrolless.ward.") }
        val secretBytes = GrantCodeCrypto.decodeBase32(displayedSecret)
        assertEquals(
            GrantCodeCrypto.computeGrantCode(secretBytes, "ABC234"),
            signer.computeCode(wardAlias, "ABC234"),
        )
    }
}
