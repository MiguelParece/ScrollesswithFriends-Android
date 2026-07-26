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

/**
 * Holds pairing secrets and computes grant codes with them. The production implementation
 * backs this with the Android Keystore so an imported secret can never be read back —
 * only used to sign — which is what makes grants unforgeable on a non-rooted device.
 */
interface GrantCodeSigner {

    /**
     * Imports [secret] under [alias] and wipes the array in place afterwards.
     */
    fun importSecret(alias: String, secret: ByteArray)

    /**
     * Computes the grant code for [challenge] with the key stored under [alias],
     * or null when no such key exists.
     */
    fun computeCode(alias: String, challenge: String): String?

    fun deleteKey(alias: String)
}
