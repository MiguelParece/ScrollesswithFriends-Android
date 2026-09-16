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
package com.scrolless.app.core.minimal

import com.scrolless.app.core.guard.ProtectedPackages

/**
 * Decides which packages survive while Block All is the chosen mode.
 *
 * The inverse of [com.scrolless.app.core.model.BlockableApp]: instead of naming what to
 * close, it names the little that stays open and closes everything else. The packages that
 * may never be closed live in [ProtectedPackages], shared with Social Media mode.
 */
object MinimalModeAllowlist {

    /**
     * @param userAllowed the apps ticked in the picker.
     * @param launchablePackageIds every package with a launcher icon, which is exactly what
     *   the app picker can offer. Anything outside it is unreachable in the picker, so it
     *   must never be kicked — see the rule below. An empty set disables kicking entirely,
     *   matching how an unresolvable launcher disables the feature.
     */
    fun allows(
        packageId: String,
        userAllowed: Set<String>,
        launcherPackageIds: Set<String>,
        imePackageId: String?,
        ownPackageId: String,
        launchablePackageIds: Set<String>,
    ): Boolean = when {
        ProtectedPackages.isProtected(packageId, launcherPackageIds, imePackageId, ownPackageId) -> true

        packageId in userAllowed -> true

        // Nothing the picker cannot offer may be closed. Fingerprint and face prompts,
        // permission dialogs, share sheets, autofill and carrier surfaces have no launcher
        // icon, so the user has no way to tick them — kicking them would be a block the user
        // can neither predict nor undo, and it takes the app underneath down with it.
        else -> packageId !in launchablePackageIds
    }
}
