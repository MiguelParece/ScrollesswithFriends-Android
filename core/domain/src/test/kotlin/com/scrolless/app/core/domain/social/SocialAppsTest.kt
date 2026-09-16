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
package com.scrolless.app.core.domain.social

import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.guard.ProtectedPackages
import com.scrolless.app.core.social.SocialApps
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LAUNCHER = "com.oem.launcher"
private const val KEYBOARD = "com.oem.keyboard"
private const val OWN = "com.scrolless.app.debug"

class SocialAppsTest : BaseTest() {

    private fun blocks(packageId: String, blocked: Set<String> = SocialApps.DEFAULT_PACKAGES) = SocialApps.blocks(
        packageId = packageId,
        blockedPackages = blocked,
        launcherPackageIds = setOf(LAUNCHER),
        imePackageId = KEYBOARD,
        ownPackageId = OWN,
    )

    @Test
    fun aListedAppIsBlocked() {
        assertTrue(blocks("com.instagram.android"))
        assertTrue(blocks("com.zhiliaoapp.musically"))
    }

    @Test
    fun anUnlistedAppIsLeftAlone() {
        assertFalse(blocks("com.android.chrome"))
        assertFalse(blocks("com.whatsapp"))
    }

    /**
     * The list is editable, so nothing stops a user ticking their own dialer. The safety core
     * has to win over the user's list exactly as it does for the allowlist.
     */
    @Test
    fun theSafetyCoreWinsOverTheUsersOwnList() {
        ProtectedPackages.CORE_PACKAGES.forEach { corePackage ->
            assertFalse(
                "$corePackage must never be closed, even if listed",
                blocks(corePackage, blocked = setOf(corePackage)),
            )
        }
        assertFalse(blocks(LAUNCHER, blocked = setOf(LAUNCHER)))
        assertFalse(blocks(KEYBOARD, blocked = setOf(KEYBOARD)))
        assertFalse(blocks(OWN, blocked = setOf(OWN)))
    }

    @Test
    fun anEmptyListBlocksNothing() {
        assertFalse(blocks("com.instagram.android", blocked = emptySet()))
    }

    /** Documented decision, asserted so it is a choice rather than an oversight. */
    @Test
    fun youTubeIsNotBlockedByDefault() {
        assertFalse(blocks("com.google.android.youtube"))
    }

    @Test
    fun defaultsAreWellFormedPackageNames() {
        SocialApps.DEFAULT_PACKAGES.forEach { packageId ->
            assertTrue("$packageId should be lowercase and trimmed", packageId == packageId.lowercase().trim())
            // One dot is enough: com.pinterest and com.tumblr are two-segment package names.
            assertTrue("$packageId should look like a package name", packageId.count { it == '.' } >= 1)
            assertFalse("$packageId should not be blank", packageId.isBlank())
        }
    }
}
