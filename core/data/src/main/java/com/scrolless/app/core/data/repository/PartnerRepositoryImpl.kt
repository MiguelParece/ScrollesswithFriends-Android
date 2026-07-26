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

import com.scrolless.app.core.blocking.time.TimeProvider
import com.scrolless.app.core.data.database.dao.PartnerLinkDao
import com.scrolless.app.core.data.database.model.PartnerLinkEntity
import com.scrolless.app.core.data.database.model.asEntity
import com.scrolless.app.core.data.database.model.asModel
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.core.model.PartnerRole
import com.scrolless.app.core.repository.PartnerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PartnerRepositoryImpl @Inject constructor(private val partnerLinkDao: PartnerLinkDao, private val timeProvider: TimeProvider) :
    PartnerRepository {

    override fun getByRole(role: PartnerRole): Flow<List<PartnerLink>> =
        partnerLinkDao.getByRole(role).map { entities -> entities.map(PartnerLinkEntity::asModel) }

    override suspend fun add(name: String, keystoreAlias: String, role: PartnerRole) {
        partnerLinkDao.insert(
            PartnerLinkEntity(
                name = name,
                keystoreAlias = keystoreAlias,
                role = role,
                createdAt = timeProvider.currentTimeInMillis(),
            ),
        )
    }

    override suspend fun delete(link: PartnerLink) {
        partnerLinkDao.deleteById(link.id)
    }
}
