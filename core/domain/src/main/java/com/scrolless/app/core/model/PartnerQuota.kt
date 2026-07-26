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
 * The three daily quota windows of the Partner Quota block option.
 */
enum class QuotaWindow {
    /** 05:00–12:00 */
    MORNING,

    /** 12:00–18:00 */
    AFTERNOON,

    /** 18:00–05:00, spanning midnight into the next day. */
    NIGHT,
}

/**
 * Persisted state of the Partner Quota block option.
 *
 * @property windowKey Identifier of the window the counters belong to, e.g. `2026-07-26|NIGHT`.
 *   The date part is the day the window STARTED, so the night window keeps one key past midnight.
 * @property usedMillis Milliseconds of blocked content consumed inside the current window.
 * @property grantedMillis Extra allowance granted by partners for the current window.
 * @property anchorWallMillis Wall-clock anchor for clock-manipulation detection.
 * @property anchorElapsedMillis Elapsed-realtime anchor taken together with [anchorWallMillis].
 * @property anchorBootCount Boot count at anchor time, -1 when unavailable.
 */
data class PartnerQuotaState(
    val windowKey: String,
    val usedMillis: Long,
    val grantedMillis: Long,
    val anchorWallMillis: Long,
    val anchorElapsedMillis: Long,
    val anchorBootCount: Int,
) {
    companion object {
        val EMPTY = PartnerQuotaState(
            windowKey = "",
            usedMillis = 0L,
            grantedMillis = 0L,
            anchorWallMillis = 0L,
            anchorElapsedMillis = 0L,
            anchorBootCount = -1,
        )
    }
}
