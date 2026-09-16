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

import kotlinx.coroutines.flow.Flow

/**
 * The apps Social Media mode closes.
 *
 * Its own store rather than a flag per app on the settings row, for the same reason the
 * minimal-mode allowlist has one: this is a collection, and `user_settings` is a single row
 * of scalars.
 */
interface SocialBlocklistStore {

    fun getBlockedApps(): Flow<Set<String>>
    suspend fun blockApp(packageId: String)
    suspend fun unblockApp(packageId: String)
}
