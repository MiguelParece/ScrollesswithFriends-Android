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

import androidx.compose.runtime.Immutable

@Immutable
data class InstalledApp(val packageId: String, val label: String)

/**
 * Lists the apps the user could pick from for the minimal-mode allowlist.
 *
 * Only apps with a launcher entry, which is what the `<queries>` element in the app manifest
 * grants visibility to. The app does not hold `QUERY_ALL_PACKAGES` and should not: seeing
 * every installed package is a far broader capability than this screen needs.
 *
 * No icons — label and package id are enough to pick with, and loading a drawable per app
 * would pull platform image types across the module boundary for a screen visited rarely.
 */
interface InstalledAppsProvider {

    /** Launchable apps sorted by display label. Safe to call off the main thread only. */
    suspend fun launchableApps(): List<InstalledApp>
}
