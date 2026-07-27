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
package com.scrolless.app.core.blocking.grace

/**
 * Grants a short budget of "passing through" time on content that is otherwise blocked.
 *
 * Instagram opens on the feed, so blocking it outright would put DMs out of reach. Each
 * visit to the host app earns one budget, spent cumulatively across every dwell in that
 * visit — walking through the feed to reach the inbox is free, sitting on it is not, and
 * bouncing between tabs does not earn a fresh budget.
 *
 * Confined to the caller's thread (the accessibility service's main thread); not synchronized.
 */
class ContentGracePeriodGate(private val budgetMillis: Long, private val elapsedRealtimeMillis: () -> Long) {

    private var remainingBudgetMillis: Long = budgetMillis
    private var dwellStartedAtMillis: Long? = null

    /** The host app came to the foreground: a new visit earns a fresh budget. */
    fun onHostAppEntered() {
        remainingBudgetMillis = budgetMillis
        dwellStartedAtMillis = null
    }

    /** The host app left the foreground: bank whatever dwell was in flight. */
    fun onHostAppExited() = onContentGone()

    /**
     * Grace-eligible content is on screen.
     *
     * @return remaining grace in millis; 0 means the budget is spent and the content
     *   should be treated as detected.
     */
    fun onContentVisible(): Long {
        if (remainingBudgetMillis <= 0L) return 0L

        val now = elapsedRealtimeMillis()
        val startedAt = dwellStartedAtMillis ?: now.also { dwellStartedAtMillis = it }

        val spent = (now - startedAt).coerceAtLeast(0L)
        val remaining = remainingBudgetMillis - spent
        if (remaining <= 0L) {
            remainingBudgetMillis = 0L
            dwellStartedAtMillis = null
            return 0L
        }
        return remaining
    }

    /** The content left the screen: charge the dwell against the budget. */
    fun onContentGone() {
        val startedAt = dwellStartedAtMillis ?: return
        val spent = (elapsedRealtimeMillis() - startedAt).coerceAtLeast(0L)
        remainingBudgetMillis = (remainingBudgetMillis - spent).coerceAtLeast(0L)
        dwellStartedAtMillis = null
    }
}
