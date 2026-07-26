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

import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.partner.GrantCodeCrypto
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrantCodeCryptoTest : BaseTest() {

    private fun hexToBytes(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun hmacSha256_matchesRfc4231TestCase1() {
        // RFC 4231, test case 1.
        val key = ByteArray(20) { 0x0b }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val digest = mac.doFinal("Hi There".encodeToByteArray())

        assertArrayEquals(
            hexToBytes("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"),
            digest,
        )
    }

    @Test
    fun truncateToCode_matchesHandComputedFixture() {
        // Digest of RFC 4231 TC1: last nibble 0x7 -> offset 7, bytes 53 5c a8 af,
        // (0x535ca8af = 1398581423) % 10^8 = 98581423.
        val digest = hexToBytes("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7")
        assertEquals("98581423", GrantCodeCrypto.truncateToCode(digest))
    }

    @Test
    fun computeGrantCode_isDeterministicAndEightDigits() {
        val secret = ByteArray(16) { it.toByte() }
        val first = GrantCodeCrypto.computeGrantCode(secret, "ABC234")
        val second = GrantCodeCrypto.computeGrantCode(secret, "ABC234")
        val other = GrantCodeCrypto.computeGrantCode(secret, "ABC235")

        assertEquals(first, second)
        assertEquals(GrantCodeCrypto.CODE_DIGITS, first.length)
        assertTrue(first.all { it.isDigit() })
        assertFalse(first == other)
    }

    @Test
    fun computeGrantCode_normalizesChallengeInput() {
        val secret = ByteArray(16) { it.toByte() }
        val canonical = GrantCodeCrypto.computeGrantCode(secret, "ABCDEF")
        val sloppy = GrantCodeCrypto.computeGrantCode(secret, " abc-def ")

        assertEquals(canonical, sloppy)
    }

    @Test
    fun base32_roundTripsRfc4648Vectors() {
        val vectors = mapOf(
            "f" to "MY",
            "fo" to "MZXQ",
            "foo" to "MZXW6",
            "foob" to "MZXW6YQ",
            "fooba" to "MZXW6YTB",
            "foobar" to "MZXW6YTBOI",
        )
        for ((plain, encoded) in vectors) {
            assertEquals(encoded, GrantCodeCrypto.encodeBase32(plain.encodeToByteArray()))
            assertArrayEquals(plain.encodeToByteArray(), GrantCodeCrypto.decodeBase32(encoded))
        }
    }

    @Test
    fun base32_secretRoundTripsThroughGroupedDisplay() {
        val secret = GrantCodeCrypto.generateSecret(SecureRandom())
        val displayed = GrantCodeCrypto.formatGrouped(GrantCodeCrypto.encodeBase32(secret))

        assertArrayEquals(secret, GrantCodeCrypto.decodeBase32(displayed))
    }

    @Test
    fun normalize_mapsAmbiguousCharactersAndStripsSeparators() {
        assertEquals("IOAB", GrantCodeCrypto.normalize(" 10-ab "))
    }

    @Test
    fun generateChallenge_hasExpectedShape() {
        val challenge = GrantCodeCrypto.generateChallenge(SecureRandom())
        assertEquals(GrantCodeCrypto.CHALLENGE_LENGTH, challenge.length)
        assertEquals(challenge, GrantCodeCrypto.normalize(challenge))
    }

    @Test
    fun constantTimeEquals_basicBehaviour() {
        assertTrue(GrantCodeCrypto.constantTimeEquals("12345678", "12345678"))
        assertFalse(GrantCodeCrypto.constantTimeEquals("12345678", "12345679"))
        assertFalse(GrantCodeCrypto.constantTimeEquals("1234567", "12345678"))
    }
}
