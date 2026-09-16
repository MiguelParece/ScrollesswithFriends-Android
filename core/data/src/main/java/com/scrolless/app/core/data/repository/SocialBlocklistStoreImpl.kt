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

import com.scrolless.app.core.data.database.dao.SocialBlockedAppDao
import com.scrolless.app.core.data.database.model.SocialBlockedAppEntity
import com.scrolless.app.core.repository.SocialBlocklistStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SocialBlocklistStoreImpl @Inject constructor(private val socialBlockedAppDao: SocialBlockedAppDao) : SocialBlocklistStore {

    override fun getBlockedApps(): Flow<Set<String>> = socialBlockedAppDao.observeAll().map { it.toSet() }

    override suspend fun blockApp(packageId: String) {
        socialBlockedAppDao.insert(SocialBlockedAppEntity(packageId = packageId))
    }

    override suspend fun unblockApp(packageId: String) {
        socialBlockedAppDao.delete(packageId)
    }
}
