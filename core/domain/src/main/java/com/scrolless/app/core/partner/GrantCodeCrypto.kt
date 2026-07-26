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

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure crypto primitives of the partner grant scheme. No Android dependencies, so every
 * operation here is verifiable with plain JVM unit tests.
 *
 * A grant code is `HMAC-SHA256(secret, "SCROLLESS-GRANT-v1|" + challenge)` truncated to
 * 8 decimal digits with the RFC 4226 dynamic-truncation scheme. The secret is exchanged
 * once at pairing as unpadded RFC 4648 base32.
 */
object GrantCodeCrypto {

    const val SECRET_LENGTH_BYTES = 16
    const val CHALLENGE_LENGTH = 6
    const val CODE_DIGITS = 8

    private const val MESSAGE_PREFIX = "SCROLLESS-GRANT-v1|"
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val GROUP_SIZE = 4

    fun generateSecret(random: SecureRandom): ByteArray = ByteArray(SECRET_LENGTH_BYTES).also(random::nextBytes)

    fun generateChallenge(random: SecureRandom): String = buildString {
        repeat(CHALLENGE_LENGTH) {
            append(BASE32_ALPHABET[random.nextInt(BASE32_ALPHABET.length)])
        }
    }

    /**
     * Uppercases, strips separators/whitespace and maps the visually ambiguous 0→O and 1→I
     * so hand-typed input survives sloppy transcription.
     */
    fun normalize(input: String): String = input
        .uppercase()
        .replace("0", "O")
        .replace("1", "I")
        .filter { it in BASE32_ALPHABET }

    fun encodeBase32(bytes: ByteArray): String {
        val output = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                output.append(BASE32_ALPHABET[(buffer shr (bitsLeft - 5)) and 0x1F])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            output.append(BASE32_ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return output.toString()
    }

    fun decodeBase32(text: String): ByteArray {
        val normalized = normalize(text)
        require(normalized.isNotEmpty()) { "Empty base32 input" }
        val output = ArrayList<Byte>(normalized.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        for (char in normalized) {
            buffer = (buffer shl 5) or BASE32_ALPHABET.indexOf(char)
            bitsLeft += 5
            if (bitsLeft >= 8) {
                output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }

    /** `ABCDEFGH` → `ABCD-EFGH` for readable one-time display and entry. */
    fun formatGrouped(text: String): String = text.chunked(GROUP_SIZE).joinToString("-")

    fun buildMessage(challenge: String): ByteArray = (MESSAGE_PREFIX + normalize(challenge)).encodeToByteArray()

    fun computeGrantCode(secret: ByteArray, challenge: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret, HMAC_ALGORITHM))
        return truncateToCode(mac.doFinal(buildMessage(challenge)))
    }

    /** RFC 4226 dynamic truncation of an HMAC digest to [CODE_DIGITS] decimal digits. */
    fun truncateToCode(hmac: ByteArray): String {
        val offset = hmac.last().toInt() and 0x0F
        val binary = ((hmac[offset].toInt() and 0x7F) shl 24) or
            ((hmac[offset + 1].toInt() and 0xFF) shl 16) or
            ((hmac[offset + 2].toInt() and 0xFF) shl 8) or
            (hmac[offset + 3].toInt() and 0xFF)
        var modulus = 1
        repeat(CODE_DIGITS) { modulus *= 10 }
        return (binary % modulus).toString().padStart(CODE_DIGITS, '0')
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
