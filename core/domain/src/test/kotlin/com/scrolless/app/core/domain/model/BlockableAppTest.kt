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
package com.scrolless.app.core.domain.model

import com.scrolless.app.core.domain.BaseTest
import com.scrolless.app.core.model.BlockableApp
import com.scrolless.app.core.model.DetectionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockableAppTest : BaseTest() {

    /**
     * The service resolves an app with `entries.firstNotNullOfOrNull`, so for the two
     * entries that share `com.instagram.android` the declaration order is the tie-break
     * and Reels has to win. A future alphabetical reorder would break detection silently.
     */
    @Test
    fun reelsIsDeclaredBeforeTheInstagramFeed() {
        val entries = BlockableApp.entries
        assertTrue(entries.indexOf(BlockableApp.REELS) < entries.indexOf(BlockableApp.INSTAGRAM_FEED))
    }

    /**
     * Enum names are persisted into `session_segments.app`; a rename or removal makes old
     * rows unreadable. Changing this list has to be a deliberate act with a migration.
     */
    @Test
    fun enumNamesArePinned() {
        assertEquals(
            listOf("REELS", "INSTAGRAM_FEED", "SHORTS", "TIKTOK", "FACEBOOK", "FACEBOOK_LITE", "SNAPCHAT"),
            BlockableApp.entries.map { it.name },
        )
    }

    /**
     * Captured from a real device: `feed_tab` exists on the profile, Explore and inbox
     * screens too, and is only selected on the home feed. Dropping `requireSelected`
     * would make every Instagram screen count as scrolling.
     */
    @Test
    fun instagramFeedIsDetectedByTheSelectedHomeTab() {
        val method = BlockableApp.INSTAGRAM_FEED.getDetectionMethod()

        assertTrue(method is DetectionMethod.ViewId)
        val viewId = method as DetectionMethod.ViewId
        assertEquals("feed_tab", viewId.viewId)
        assertTrue("the home tab is only meaningful when selected", viewId.requireSelected)
    }

    /** Reels must keep matching the clips viewer, which the feed signal never sees. */
    @Test
    fun reelsIsDetectedByTheClipsViewer() {
        val method = BlockableApp.REELS.getDetectionMethod()

        assertEquals(DetectionMethod.ViewId("clips_viewer_view_pager"), method)
    }

    @Test
    fun everyEntryDeclaresAtLeastOnePackage() {
        BlockableApp.entries.forEach { app ->
            assertTrue("${app.name} has no package ids", app.getPackageIds().isNotEmpty())
        }
    }
}
