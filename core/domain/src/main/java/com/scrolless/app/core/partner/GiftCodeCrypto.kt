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
 * Creates and validates transferable "+15 minutes" gift codes.
 *
 * A gift code is `base32(nonce ‖ issuedAtMinutes ‖ mac)` where the MAC is HMAC-SHA256
 * over the nonce and timestamp with [EMBEDDED_KEY], truncated to 8 bytes. Any Scrolless
 * install can generate and validate codes; single-use is enforced by the receiving
 * device remembering redeemed nonces.
 *
 * HONESTY NOTE: the key ships inside this open-source app, so the MAC proves only that
 * a code is well-formed — it is tamper/typo detection and casual-cheat friction, NOT
 * cryptographic proof that another person generated it. This is a deliberate design
 * choice favouring zero-setup sharing over enforcement.
 */
object GiftCodeCrypto {

    const val GIFT_MINUTES = 15

    private const val MESSAGE_PREFIX = "SCROLLESS-GIFT-v1|"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val GROUP_SIZE = 4

    private const val NONCE_BYTES = 8
    private const val TIME_BYTES = 4
    private const val MAC_BYTES = 8
    private const val TOKEN_BYTES = NONCE_BYTES + TIME_BYTES + MAC_BYTES
    private const val TOKEN_CHARS = 32 // ceil(20 bytes * 8 / 5)

    // Public by design (see class doc). Changing it invalidates all codes in flight.
    private val EMBEDDED_KEY = "scrolless-gift-format-key-v1:c4e1b2a9d87f60e3".encodeToByteArray()

    sealed class ParseResult {
        /** Structurally valid code. Single-use and expiry are the caller's checks. */
        data class Valid(val nonce: String, val issuedAtMillis: Long) : ParseResult()

        data object Malformed : ParseResult()
    }

    /** Generates a fresh grouped gift code, e.g. `XXXX-XXXX-…` (8 groups). */
    fun createGiftCode(random: SecureRandom, nowMillis: Long): String {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val issuedAtMinutes = (nowMillis / 60_000L).toInt()

        val payload = ByteArray(NONCE_BYTES + TIME_BYTES)
        nonce.copyInto(payload)
        writeIntBE(payload, NONCE_BYTES, issuedAtMinutes)

        val token = payload + mac(payload).copyOf(MAC_BYTES)
        return formatGrouped(encodeBase32(token))
    }

    /**
     * Extracts and validates a gift code from arbitrary pasted text (e.g. a whole
     * WhatsApp message). Any run of base32-ish characters of the right length is tried.
     */
    fun parse(pastedText: String): ParseResult {
        // Candidates are unbroken runs of base32 characters and dashes; normalizing a
        // run must yield exactly one token's worth of characters. Ordinary words in the
        // surrounding message form their own (wrong-length) runs and drop out here.
        val candidates = Regex("[A-Za-z0-9-]+")
            .findAll(pastedText)
            .map { normalize(it.value) }
            .filter { it.length == TOKEN_CHARS }

        for (candidate in candidates) {
            val token = decodeBase32(candidate) ?: continue
            if (token.size != TOKEN_BYTES) continue

            val payload = token.copyOfRange(0, NONCE_BYTES + TIME_BYTES)
            val expectedMac = mac(payload).copyOf(MAC_BYTES)
            val actualMac = token.copyOfRange(NONCE_BYTES + TIME_BYTES, TOKEN_BYTES)
            if (!constantTimeEquals(expectedMac, actualMac)) continue

            val nonce = encodeBase32(token.copyOfRange(0, NONCE_BYTES))
            val issuedAtMinutes = readIntBE(payload, NONCE_BYTES)
            return ParseResult.Valid(nonce = nonce, issuedAtMillis = issuedAtMinutes.toLong() * 60_000L)
        }
        return ParseResult.Malformed
    }

    private fun mac(payload: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(EMBEDDED_KEY, HMAC_ALGORITHM))
        mac.update(MESSAGE_PREFIX.encodeToByteArray())
        return mac.doFinal(payload)
    }

    private fun writeIntBE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    private fun readIntBE(source: ByteArray, offset: Int): Int = ((source[offset].toInt() and 0xFF) shl 24) or
        ((source[offset + 1].toInt() and 0xFF) shl 16) or
        ((source[offset + 2].toInt() and 0xFF) shl 8) or
        (source[offset + 3].toInt() and 0xFF)

    /** Uppercases, strips separators and maps ambiguous 0→O / 1→I. */
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

    fun decodeBase32(text: String): ByteArray? {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return null
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

    fun formatGrouped(text: String): String = text.chunked(GROUP_SIZE).joinToString("-")

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
