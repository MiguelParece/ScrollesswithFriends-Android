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

/**
 * Persisted state of the strict-mode time lock.
 *
 * @property untilAtMillis Wall-clock end of the lock; 0 when never armed.
 * @property anchorWallMillis Wall clock captured at arm/extend/re-anchor time.
 * @property anchorElapsedMillis Elapsed realtime captured together with the wall anchor —
 *   within one boot, expiry is measured against this, making clock edits irrelevant.
 * @property anchorBootCount Boot count at anchor time, -1 when unavailable.
 */
data class StrictModeState(
    val untilAtMillis: Long = 0L,
    val anchorWallMillis: Long = 0L,
    val anchorElapsedMillis: Long = 0L,
    val anchorBootCount: Int = -1,
) {
    companion object {
        val EMPTY = StrictModeState()
    }
}

sealed interface StrictArmResult {
    /** Lock armed (or extended) until [untilAtMillis]. */
    data class Armed(val untilAtMillis: Long) : StrictArmResult

    /** Requested duration would end earlier than the current lock — extending only goes forward. */
    data object RejectedWouldShorten : StrictArmResult

    data object RejectedInvalidDuration : StrictArmResult
}
