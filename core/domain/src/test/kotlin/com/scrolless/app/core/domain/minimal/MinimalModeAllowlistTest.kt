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
package com.scrolless.app.core.domain.minimal

import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.minimal.MinimalModeAllowlist
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LAUNCHER = "com.oem.launcher"
private const val SECOND_LAUNCHER = "com.other.launcher"
private const val KEYBOARD = "com.oem.keyboard"
private const val OWN = "com.scrolless.app.debug"
private const val WHATSAPP = "com.whatsapp"

class MinimalModeAllowlistTest : BaseTest() {

    private fun allows(packageId: String, userAllowed: Set<String> = emptySet()) = MinimalModeAllowlist.allows(
        packageId = packageId,
        userAllowed = userAllowed,
        launcherPackageIds = setOf(LAUNCHER, SECOND_LAUNCHER),
        imePackageId = KEYBOARD,
        ownPackageId = OWN,
    )

    @Test
    fun anUnknownAppIsRefused() {
        assertFalse(allows("com.instagram.android"))
    }

    @Test
    fun aPickedAppIsAllowed() {
        assertTrue(allows(WHATSAPP, userAllowed = setOf(WHATSAPP)))
        assertFalse(allows("com.instagram.android", userAllowed = setOf(WHATSAPP)))
    }

    @Test
    fun theLauncherIsAllowedSoHomeIsNotAKickLoop() {
        assertTrue(allows(LAUNCHER))
    }

    /** With no default launcher, home opens a chooser; every candidate has to survive. */
    @Test
    fun everyLauncherCandidateIsAllowed() {
        assertTrue(allows(SECOND_LAUNCHER))
    }

    /** The chooser itself, permission dialogs and system alerts all run as "android". */
    @Test
    fun theSystemPackageIsAllowed() {
        assertTrue(allows("android"))
    }

    /** Typing inside an allowed app must not read as opening a blocked one. */
    @Test
    fun theKeyboardIsAllowed() {
        assertTrue(allows(KEYBOARD))
    }

    @Test
    fun scrollessItselfIsAllowed() {
        assertTrue(allows(OWN))
    }

    /** A window that reports no package cannot be classified, so it is left alone. */
    @Test
    fun blankPackageIsAllowed() {
        assertTrue(allows(""))
        assertTrue(allows("   "))
    }

    @Test
    fun theSafetyCoreIsAlwaysAllowed() {
        MinimalModeAllowlist.CORE_PACKAGES.forEach { corePackage ->
            assertTrue("$corePackage must never be kicked", allows(corePackage))
        }
    }

    @Test
    fun callsAndAlarmsAreInTheSafetyCore() {
        assertTrue(allows("com.android.server.telecom"))
        assertTrue(allows("com.android.incallui"))
        assertTrue(allows("com.android.emergency"))
        assertTrue(allows("com.android.systemui"))
        assertTrue(allows("com.google.android.deskclock"))
    }

    /** Reaching Settings is how blocking gets switched off; it is opt-in, not core. */
    @Test
    fun settingsIsNotInTheSafetyCore() {
        assertFalse(allows("com.android.settings"))
        assertTrue(allows("com.android.settings", userAllowed = setOf("com.android.settings")))
    }

    /** A device with no resolvable launcher or keyboard must not accidentally match. */
    @Test
    fun emptyLauncherSetAndNullImeMatchNothing() {
        assertFalse(
            MinimalModeAllowlist.allows(
                packageId = "com.instagram.android",
                userAllowed = emptySet(),
                launcherPackageIds = emptySet(),
                imePackageId = null,
                ownPackageId = OWN,
            ),
        )
    }
}
