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
package com.scrolless.app.core.social

import com.scrolless.app.core.guard.ProtectedPackages

/**
 * The apps Social Media mode closes, and the list it starts from.
 *
 * Unlike [com.scrolless.app.core.model.BlockableApp], which names a *surface* inside an app
 * and needs a view id to find it, this names whole packages. Closing a package needs nothing
 * but its name, so the list costs nothing to extend and does not rot when an app is
 * redesigned.
 */
object SocialApps {

    /**
     * Seeded into the database the first time Social Media mode exists on a device, and
     * editable from there — so this is a starting point, not a fixed policy.
     *
     * Deliberately absent: YouTube. It is a video platform people also use for cooking and
     * repairs, and closing it outright surprises more than it helps. Its endless surface,
     * Shorts, stays blocked anyway — Social Media mode keeps short-form blocking running, so
     * Shorts is closed inside a YouTube that still opens. Anyone who disagrees can tick it.
     */
    val DEFAULT_PACKAGES: Set<String> = setOf(
        // Instagram and Threads
        "com.instagram.android",
        "com.instagram.barcelona",

        // TikTok, under each of the names it ships as
        "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically.go",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme",

        // Meta
        "com.facebook.katana",
        "com.facebook.lite",

        // X
        "com.twitter.android",
        "com.twitter.android.lite",

        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "com.linkedin.android",
        "com.tumblr",
    )

    /**
     * Whether Social Media mode should close [packageId].
     *
     * Goes through [ProtectedPackages] rather than trusting [blockedPackages] blindly: the
     * list is editable from the app picker, so nothing stops a user ticking their dialer,
     * and the safety core has to win over the user's own list the same way it does in
     * Block All.
     */
    fun blocks(
        packageId: String,
        blockedPackages: Set<String>,
        launcherPackageIds: Set<String>,
        imePackageId: String?,
        ownPackageId: String,
    ): Boolean = when {
        ProtectedPackages.isProtected(packageId, launcherPackageIds, imePackageId, ownPackageId) -> false
        else -> packageId in blockedPackages
    }
}
