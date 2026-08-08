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
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.data.database.dao.MinimalModeAllowedAppDao
import com.scrolless.app.core.data.database.dao.MinimalModeWindowDao
import com.scrolless.app.core.data.database.model.MinimalModeAllowedAppEntity
import com.scrolless.app.core.data.database.model.MinimalModeWindowEntity
import com.scrolless.app.core.minimal.MinimalModeWindow
import com.scrolless.app.core.repository.MinimalModeStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MinimalModeStoreImpl @Inject constructor(
    private val allowedAppDao: MinimalModeAllowedAppDao,
    private val windowDao: MinimalModeWindowDao,
) : MinimalModeStore {

    override fun getAllowedApps(): Flow<Set<String>> = allowedAppDao.observeAll().map { it.toSet() }

    override suspend fun allowApp(packageId: String) {
        allowedAppDao.insert(MinimalModeAllowedAppEntity(packageId = packageId))
    }

    override suspend fun disallowApp(packageId: String) {
        allowedAppDao.delete(packageId)
    }

    override fun getWindows(): Flow<List<MinimalModeWindow>> = windowDao.observeAll().map { entities ->
        entities.map { MinimalModeWindow(startMinuteOfDay = it.startMinute, endMinuteOfDay = it.endMinute) }
    }

    override suspend fun setWindows(windows: List<MinimalModeWindow>) {
        windowDao.replaceAll(
            windows.map { MinimalModeWindowEntity(startMinute = it.startMinuteOfDay, endMinute = it.endMinuteOfDay) },
        )
    }
}
