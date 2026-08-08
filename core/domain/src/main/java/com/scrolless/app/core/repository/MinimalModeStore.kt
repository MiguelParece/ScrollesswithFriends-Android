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
package com.scrolless.app.core.repository

import com.scrolless.app.core.minimal.MinimalModeWindow
import kotlinx.coroutines.flow.Flow

/**
 * The two collections minimal mode owns: which apps survive, and when it is in force.
 *
 * Separate from [UserSettingsStore] because those are rows in their own tables rather than
 * columns on the single settings row. The master switch and the clock anchor are scalars and
 * live on [UserSettingsStore] with the rest.
 */
interface MinimalModeStore {

    fun getAllowedApps(): Flow<Set<String>>
    suspend fun allowApp(packageId: String)
    suspend fun disallowApp(packageId: String)

    fun getWindows(): Flow<List<MinimalModeWindow>>

    /** Replaces the whole schedule; windows are edited as a set, not individually. */
    suspend fun setWindows(windows: List<MinimalModeWindow>)
}
