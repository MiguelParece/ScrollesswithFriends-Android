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
package com.scrolless.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.designsystem.theme.LocalSharedTransitionScope
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.feature.home.HomeScreen
import com.scrolless.app.feature.settings.SettingsScreen
import com.scrolless.app.feature.settings.minimal.MinimalModeScreen
import com.scrolless.app.util.requestAppReview
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingGiftCode by mutableStateOf<String?>(null)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractGiftCode(intent)

        setContent {

            val appState: ScrollessAppState = rememberScrollessAppState()

            ScrollessTheme {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavDisplay(
                            appState.backStack,
                            onBack = { appState.navigateBack() },
                            entryProvider = entryProvider {
                                entry<ScrollessRoute.Home> {
                                    HomeScreen(
                                        onNavigateToSettings = appState::navigateToSettings,
                                        accessibilityServiceClass = ScrollessBlockAccessibilityService::class.java,
                                        pendingGiftCode = pendingGiftCode,
                                        onGiftCodeConsumed = { pendingGiftCode = null },
                                        onRequestAppReview = ::requestAppReview,
                                    )
                                }
                                entry<ScrollessRoute.Settings> {
                                    SettingsScreen(
                                        onNavigateBack = appState::navigateBack,
                                        onNavigateToMinimalMode = appState::navigateToMinimalMode,
                                    )
                                }
                                entry<ScrollessRoute.MinimalMode> {
                                    MinimalModeScreen(
                                        onNavigateBack = appState::navigateBack,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractGiftCode(intent)
    }

    /**
     * A tapped gift link (`scrolless://gift/<code>`) lands here; the raw URI is handed
     * to the redeem flow, which extracts the code itself.
     */
    private fun extractGiftCode(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "scrolless" && data.host == "gift") {
            Timber.i("Gift deep link received")
            pendingGiftCode = data.toString()
        }
    }
}
