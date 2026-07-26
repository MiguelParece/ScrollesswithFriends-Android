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
import com.scrolless.app.core.partner.GiftCodeCrypto
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftCodeCryptoTest : BaseTest() {

    private val random = SecureRandom()
    private val nowMillis = 1_785_000_000_000L // fixed reference instant

    @Test
    fun createdCode_parsesBackWithSameTimestampMinute() {
        val code = GiftCodeCrypto.createGiftCode(random, nowMillis)

        val result = GiftCodeCrypto.parse(code)

        assertTrue(result is GiftCodeCrypto.ParseResult.Valid)
        val valid = result as GiftCodeCrypto.ParseResult.Valid
        assertEquals((nowMillis / 60_000L) * 60_000L, valid.issuedAtMillis)
    }

    @Test
    fun parse_findsCodeInsideWhatsAppStyleMessage() {
        val code = GiftCodeCrypto.createGiftCode(random, nowMillis)
        val message = "🎁 Here you go, 15 more minutes! Code: $code enjoy responsibly"

        assertTrue(GiftCodeCrypto.parse(message) is GiftCodeCrypto.ParseResult.Valid)
    }

    @Test
    fun parse_survivesLowercaseAndMissingDashes() {
        val code = GiftCodeCrypto.createGiftCode(random, nowMillis)
        val mangled = code.lowercase().replace("-", "")

        assertTrue(GiftCodeCrypto.parse(mangled) is GiftCodeCrypto.ParseResult.Valid)
    }

    @Test
    fun parse_rejectsTamperedCode() {
        val code = GiftCodeCrypto.createGiftCode(random, nowMillis)
        val flipped = if (code[0] != 'A') 'A' else 'B'
        val tampered = flipped + code.substring(1)

        assertEquals(GiftCodeCrypto.ParseResult.Malformed, GiftCodeCrypto.parse(tampered))
    }

    @Test
    fun parse_rejectsGarbage() {
        assertEquals(GiftCodeCrypto.ParseResult.Malformed, GiftCodeCrypto.parse("hello there, no code here"))
        assertEquals(GiftCodeCrypto.ParseResult.Malformed, GiftCodeCrypto.parse(""))
        assertEquals(GiftCodeCrypto.ParseResult.Malformed, GiftCodeCrypto.parse("AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA"))
    }

    @Test
    fun codes_haveUniqueNonces() {
        val first = GiftCodeCrypto.parse(GiftCodeCrypto.createGiftCode(random, nowMillis))
        val second = GiftCodeCrypto.parse(GiftCodeCrypto.createGiftCode(random, nowMillis))

        val firstNonce = (first as GiftCodeCrypto.ParseResult.Valid).nonce
        val secondNonce = (second as GiftCodeCrypto.ParseResult.Valid).nonce
        assertNotEquals(firstNonce, secondNonce)
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
            assertEquals(encoded, GiftCodeCrypto.encodeBase32(plain.encodeToByteArray()))
            assertArrayEquals(plain.encodeToByteArray(), GiftCodeCrypto.decodeBase32(encoded))
        }
    }

    @Test
    fun normalize_mapsAmbiguousCharactersAndStripsSeparators() {
        assertEquals("IOAB", GiftCodeCrypto.normalize(" 10-ab "))
    }
}
