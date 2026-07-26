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
package com.scrolless.app.core.model

/**
 * Which side of an accountability pairing this device plays for a given link.
 */
enum class PartnerRole {
    /** This person grants ME extra time — their key verifies incoming grant codes. */
    PARTNER,

    /** I grant THEM extra time — the key generates codes for their challenges. */
    WARD,
}

/**
 * A paired accountability contact. The HMAC secret itself lives in the Android Keystore
 * under [keystoreAlias]; only this metadata row is stored in the database.
 */
data class PartnerLink(val id: Long, val name: String, val keystoreAlias: String, val role: PartnerRole, val createdAt: Long)
