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
import com.scrolless.app.core.data.database.model.RedeemedGiftEntity

/**
 * [androidx.room.Room] DAO for [RedeemedGiftEntity].
 */
@Dao
abstract class RedeemedGiftDao {

    /**
     * Inserts the nonce, returning -1 when it was already present. The primary-key
     * conflict makes the redeem-once check atomic.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun tryInsert(entity: RedeemedGiftEntity): Long
}
