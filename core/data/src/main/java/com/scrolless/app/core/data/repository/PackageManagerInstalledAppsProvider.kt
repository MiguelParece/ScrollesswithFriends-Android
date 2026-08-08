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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.scrolless.app.core.repository.InstalledApp
import com.scrolless.app.core.repository.InstalledAppsProvider
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageManagerInstalledAppsProvider @Inject constructor(private val context: Context) : InstalledAppsProvider {

    /**
     * Queries launcher entries rather than all installed packages, which is what the manifest
     * `<queries>` element grants. Runs on IO: reading every label touches each app's resources
     * and takes long enough to drop frames on a device with a lot installed.
     */
    override suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageId = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                InstalledApp(
                    packageId = packageId,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                )
            }
            // One package can publish several launcher activities; the picker wants one row.
            .distinctBy { it.packageId }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
