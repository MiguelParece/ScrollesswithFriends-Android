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
 * One app Social Media mode closes.
 *
 * Seeded from [com.scrolless.app.core.social.SocialApps.DEFAULT_PACKAGES] when the table is
 * created, then owned by the user. Seeding happens once, so unticking an app makes it stay
 * unticked rather than coming back on the next launch.
 */
@Entity(tableName = "social_blocked_apps")
@Immutable
data class SocialBlockedAppEntity(@PrimaryKey @ColumnInfo(name = "package_id") val packageId: String)
