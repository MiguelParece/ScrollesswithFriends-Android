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
package com.scrolless.app.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.scrolless.app.core.data.database.model.PartnerLinkEntity
import com.scrolless.app.core.model.PartnerRole
import kotlinx.coroutines.flow.Flow

/**
 * [androidx.room.Room] DAO for [PartnerLinkEntity] related operations.
 */
@Dao
abstract class PartnerLinkDao : BaseDao<PartnerLinkEntity> {

    @Query("SELECT * FROM partner_links ORDER BY created_at ASC")
    abstract fun getAll(): Flow<List<PartnerLinkEntity>>

    @Query("SELECT * FROM partner_links WHERE role = :role ORDER BY created_at ASC")
    abstract fun getByRole(role: PartnerRole): Flow<List<PartnerLinkEntity>>

    @Query("DELETE FROM partner_links WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}
