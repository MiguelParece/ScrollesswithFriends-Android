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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.scrolless.app.core.data.database.model.MinimalModeAllowedAppEntity
import com.scrolless.app.core.data.database.model.MinimalModeWindowEntity
import kotlinx.coroutines.flow.Flow

/**
 * [androidx.room.Room] DAO for [MinimalModeAllowedAppEntity].
 */
@Dao
abstract class MinimalModeAllowedAppDao {

    @Query("SELECT package_id FROM minimal_mode_allowed_apps ORDER BY package_id")
    abstract fun observeAll(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: MinimalModeAllowedAppEntity)

    @Query("DELETE FROM minimal_mode_allowed_apps WHERE package_id = :packageId")
    abstract suspend fun delete(packageId: String)
}

/**
 * [androidx.room.Room] DAO for [MinimalModeWindowEntity].
 */
@Dao
abstract class MinimalModeWindowDao {

    @Query("SELECT * FROM minimal_mode_windows ORDER BY start_minute, end_minute")
    abstract fun observeAll(): Flow<List<MinimalModeWindowEntity>>

    @Insert
    abstract suspend fun insertAll(entities: List<MinimalModeWindowEntity>)

    @Query("DELETE FROM minimal_mode_windows")
    abstract suspend fun deleteAll()

    /**
     * Swaps the whole schedule in one transaction. Windows have no identity worth preserving
     * — they are edited as a set — and a partial write here would leave the phone locked to a
     * schedule the user never chose.
     */
    @Transaction
    open suspend fun replaceAll(entities: List<MinimalModeWindowEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
