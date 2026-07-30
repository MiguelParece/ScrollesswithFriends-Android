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
package com.scrolless.app.core.model

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import androidx.compose.runtime.Immutable

// DetectionMethod holds the information to find out if blocked content is visible
// Most of the apps work by just checking if the view id is present
//  but facebook (thanks) needs to be different and only works via content descriptions which is a nice hammer
sealed class DetectionMethod {
    /**
     * @property requireSelected when true the node must also report `isSelected`. Needed for
     *   signals that exist on every screen of an app and only mean something when active,
     *   such as a bottom-navigation tab.
     */
    data class ViewId(val viewId: String, val requireSelected: Boolean = false) : DetectionMethod()
    data class ContentDescriptions(val contentDescriptions: Set<String>) : DetectionMethod()
    data class ContentDescriptionPrefix(
        val prefixes: Set<String>,
        val requireSelected: Boolean = false,
        val maxTopScreenFraction: Float? = null,
    ) : DetectionMethod()
    data class AnyOf(val detectionMethods: List<DetectionMethod>) : DetectionMethod()
}

// Declares each supported app together with the package names we match, the detection signal to look for,
//  and the exit action to use once blocked content is found.
@Immutable
enum class BlockableApp(
    private val packageIds: List<String>,
    private val detectionMethod: DetectionMethod,
    private val exitStrategy: Int,
) {
    REELS(
        packageIds = listOf("com.instagram.android"),
        detectionMethod = DetectionMethod.ViewId("clips_viewer_view_pager"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),

    /**
     * Instagram's home feed — endless scrolling that is not short-form video.
     *
     * Declared after [REELS] on purpose: both match `com.instagram.android`, and the
     * service resolves an app with `entries.firstNotNullOfOrNull`, so Reels wins the tie.
     * Never rename or remove this constant — `BlockableAppTypeConverters` persists
     * `enum.name` into `session_segments.app`, and an unknown name fails reads.
     *
     * Exits with HOME rather than BACK: BACK out of the feed can be swallowed, and the
     * periodic check does not re-arm after acting, which would leave the user unblocked.
     *
     * Detected by the home tab of the bottom navigation being selected. Device captures
     * showed this is the only signal that separates the feed from everything else: the
     * feed's list container and post headers are reused verbatim when a single post is
     * opened from a profile grid, so matching those would count reading one post as
     * scrolling. The tab is a view ID, so unlike the "For you" title it does not depend
     * on the device language.
     */
    INSTAGRAM_FEED(
        packageIds = listOf("com.instagram.android"),
        detectionMethod = DetectionMethod.ViewId("feed_tab", requireSelected = true),
        exitStrategy = GLOBAL_ACTION_HOME,
    ),
    SHORTS(
        packageIds = listOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.kids",
            "app.revanced.android.youtube",
        ),
        detectionMethod = DetectionMethod.ViewId("reel_player_page_container"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    TIKTOK(
        packageIds = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme",
            "com.zhiliaoapp.musically.go",
        ),
        detectionMethod = DetectionMethod.ViewId("player_view"),
        exitStrategy = GLOBAL_ACTION_HOME,
    ),
    FACEBOOK(
        packageIds = listOf("com.facebook.katana"),
        // Facebook needs several detection methods because there's different ways of watching reels
        // 1. By pressing on a reel in the main feed
        //      - Easy detection by the content descriptions Sticker & GIF
        // 2. By pressing the Reels tab
        //      - Here we expect the selected top navigation accessibility label to start with "Reels, tab"
        //      - The reason for not just matching "Reels" is that this text can also appear in the user feed
        detectionMethod = DetectionMethod.AnyOf(
            listOf(
                DetectionMethod.ContentDescriptions(
                    setOf(
                        "FbShortsComposerAttachmentComponentSpec_STICKER",
                        "FbShortsComposerAttachmentComponentSpec_GIF",
                    ),
                ),
                // Facebook also shows feed shelves labeled just "Reels", which should not trigger blocking.
                // so we search for the accessibility label to start with "Reels, tab" (for example "Reels, tab 2 of 6").
                DetectionMethod.ContentDescriptionPrefix(
                    prefixes = setOf("Reels, tab"),
                    requireSelected = true,
                    maxTopScreenFraction = 0.2f,
                ),
            ),
        ),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    FACEBOOK_LITE(
        packageIds = listOf("com.facebook.lite"),
        detectionMethod = DetectionMethod.ViewId("video_view"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    SNAPCHAT(
        packageIds = listOf("com.snapchat.android"),
        detectionMethod = DetectionMethod.ViewId("spotlight_container"),
        exitStrategy = GLOBAL_ACTION_BACK,
    ),
    ;

    fun getExitStrategy(): Int = exitStrategy

    fun getDetectionMethod(): DetectionMethod = detectionMethod

    fun getPackageIds(): List<String> = packageIds

    fun resolvePackage(packageName: String): String? = packageName.takeIf(::matchesPackage)

    private fun matchesPackage(packageName: String): Boolean = packageIds.any { it == packageName }
}

// Represents the specific package variant
@Immutable
data class ResolvedBlockableApp(val app: BlockableApp, val packageId: String) {
    fun getDetectionMethod(): DetectionMethod = app.getDetectionMethod()

    fun getExitStrategy(): Int = app.getExitStrategy()

    fun getViewId(detectionMethod: DetectionMethod.ViewId): String = "$packageId:id/${detectionMethod.viewId}"
}
