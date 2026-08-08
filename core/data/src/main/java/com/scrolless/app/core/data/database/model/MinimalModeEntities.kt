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
import androidx.room.PrimaryKey

/**
 * One app the user allowed through minimal mode.
 *
 * A row-per-item table rather than a column on `user_settings`, because this is the first
 * collection the app persists and `user_settings` is a single row of scalars. The package id
 * is the key, so allowing the same app twice is a no-op.
 */
@Entity(tableName = "minimal_mode_allowed_apps")
@Immutable
data class MinimalModeAllowedAppEntity(@PrimaryKey @ColumnInfo(name = "package_id") val packageId: String)

/**
 * One stretch of the day during which minimal mode is in force.
 *
 * Minutes since midnight, `0..1439`. An end that is not after the start spans midnight; see
 * [com.scrolless.app.core.minimal.MinimalModeWindow], which is what the rest of the app works
 * with.
 */
@Entity(tableName = "minimal_mode_windows")
@Immutable
data class MinimalModeWindowEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "start_minute") val startMinute: Int,
    @ColumnInfo(name = "end_minute") val endMinute: Int,
)
