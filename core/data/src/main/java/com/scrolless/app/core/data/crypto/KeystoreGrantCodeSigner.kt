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
package com.scrolless.app.core.data.crypto

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import com.scrolless.app.core.partner.GrantCodeCrypto
import com.scrolless.app.core.partner.GrantCodeSigner
import java.security.KeyStore
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import timber.log.Timber

/**
 * [GrantCodeSigner] backed by the Android Keystore. Keys are imported as non-exportable
 * HmacSHA256 signing keys: after [importSecret] returns, the raw secret exists nowhere
 * on this device.
 */
class KeystoreGrantCodeSigner : GrantCodeSigner {

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun importSecret(alias: String, secret: ByteArray) {
        try {
            keyStore().setEntry(
                alias,
                KeyStore.SecretKeyEntry(SecretKeySpec(secret, KeyProperties.KEY_ALGORITHM_HMAC_SHA256)),
                KeyProtection.Builder(KeyProperties.PURPOSE_SIGN).build(),
            )
        } finally {
            secret.fill(0)
        }
    }

    override fun computeCode(alias: String, challenge: String): String? {
        val key = try {
            keyStore().getKey(alias, null) as? SecretKey
        } catch (e: Exception) {
            Timber.w(e, "Failed to load partner key %s", alias)
            null
        } ?: return null

        val mac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
        mac.init(key)
        return GrantCodeCrypto.truncateToCode(mac.doFinal(GrantCodeCrypto.buildMessage(challenge)))
    }

    override fun deleteKey(alias: String) {
        try {
            keyStore().deleteEntry(alias)
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete partner key %s", alias)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
