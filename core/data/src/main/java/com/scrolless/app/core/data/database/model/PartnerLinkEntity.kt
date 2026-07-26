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
package com.scrolless.app.core.data.database.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.core.model.PartnerRole

@Entity(
    tableName = "partner_links",
    indices = [
        Index("keystore_alias", unique = true),
    ],
)
@Immutable
data class PartnerLinkEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "keystore_alias") val keystoreAlias: String,
    @ColumnInfo(name = "role") val role: PartnerRole,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun PartnerLinkEntity.asModel() = PartnerLink(
    id = id,
    name = name,
    keystoreAlias = keystoreAlias,
    role = role,
    createdAt = createdAt,
)

fun PartnerLink.asEntity() = PartnerLinkEntity(
    id = id,
    name = name,
    keystoreAlias = keystoreAlias,
    role = role,
    createdAt = createdAt,
)
