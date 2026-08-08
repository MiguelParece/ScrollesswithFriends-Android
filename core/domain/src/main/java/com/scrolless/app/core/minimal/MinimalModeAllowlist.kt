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

/**
 * Decides which packages survive while minimal mode is in force.
 *
 * This is the inverse of [com.scrolless.app.core.model.BlockableApp]: instead of naming what
 * to close, it names the little that stays open and closes everything else.
 *
 * The safety core below is not user-editable and not removable by any setting, because
 * getting it wrong while strict mode is armed means days without a dialer or an alarm and no
 * way back inside the app. Packages that vary per device — the launcher, the keyboard and
 * Scrolless' own build flavour — are resolved at runtime and passed in rather than guessed
 * here, and anything unknown resolves to "allow" rather than risking a phone that cannot
 * reach its own home screen.
 */
object MinimalModeAllowlist {

    /**
     * Always allowed, whatever the user picked.
     *
     * Settings is deliberately absent: reaching it is how blocking gets switched off, and
     * strict mode already guards it. Anyone who wants it can add it from the app picker.
     */
    val CORE_PACKAGES: Set<String> = setOf(
        // Status bar, notification shade, volume dialog, power menu, and on several OEMs the
        // incoming-call UI as well.
        "com.android.systemui",

        // Calls. Telecom routes them, incallui draws the in-call screen, and the rest are the
        // dialer under its various OEM names.
        "com.android.server.telecom",
        "com.android.incallui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",

        // Emergency dialing must never be reachable-by-luck.
        "com.android.phone",
        "com.android.emergency",

        // Alarms are how people wake up.
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",

        // The platform itself: the app chooser, permission dialogs and system alerts all run
        // here. Kicking it would trap the user in whatever prompt was on screen.
        "android",
    )

    /**
     * @param packageId the package owning the foreground window; blank when the window does
     *   not report one, which is treated as allowed because it cannot be classified.
     * @param launcherPackageIds every package answering the home intent, not just the default
     *   one. A device with two launchers and no default sends home to the system chooser, and
     *   a single package id would leave the other launcher unprotected against a kick loop.
     *   An empty set must disable the feature upstream rather than reach here.
     * @param imePackageId the current keyboard, so typing inside an allowed app is not
     *   mistaken for opening a blocked one.
     * @param ownPackageId Scrolless itself, which differs between the debug and release
     *   flavours and so cannot be a constant.
     */
    fun allows(
        packageId: String,
        userAllowed: Set<String>,
        launcherPackageIds: Set<String>,
        imePackageId: String?,
        ownPackageId: String,
    ): Boolean = when {
        packageId.isBlank() -> true
        packageId in CORE_PACKAGES -> true
        packageId in launcherPackageIds -> true
        packageId == imePackageId -> true
        packageId == ownPackageId -> true
        else -> packageId in userAllowed
    }
}
