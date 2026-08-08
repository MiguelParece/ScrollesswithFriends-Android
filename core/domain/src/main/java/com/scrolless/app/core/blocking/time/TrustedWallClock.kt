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
package com.scrolless.app.core.blocking.time

/**
 * Reconstructs what the wall clock *should* read, so a feature keyed to the time of day
 * cannot be moved by editing the device clock.
 *
 * [ClockAnomalyDetector] answers "can I trust this clock"; this answers "what time is it
 * really". Within a boot the answer is exact: elapsed realtime is monotonic and cannot be
 * set, so the anchor plus the elapsed delta is the true time no matter what the user typed
 * into Settings. Across a reboot elapsed realtime resets and the anchor is all that is
 * left, so the best that can be done is to refuse to go backwards.
 *
 * Deliberately not fail-closed. A schedule that locks the whole phone is the wrong place
 * for "the clock looks odd, so assume the worst" — deriving the real time is both safer
 * for the user and stricter against tampering.
 */
object TrustedWallClock {

    /**
     * @param anchorWallMillis wall clock when the anchor was taken; `<= 0` means no anchor yet.
     * @param anchorBootCount boot count at the anchor, or `-1` when the device does not expose one.
     * @return the most trustworthy estimate of the current wall clock, in epoch millis.
     */
    fun nowMillis(
        anchorWallMillis: Long,
        anchorElapsedMillis: Long,
        anchorBootCount: Int,
        wallMillis: Long,
        elapsedMillis: Long,
        bootCount: Int,
    ): Long {
        // Nothing to derive from yet: the first reading has to be taken on trust.
        if (anchorWallMillis <= 0L) return wallMillis

        val bootKnown = anchorBootCount >= 0 && bootCount >= 0
        val sameBoot = if (bootKnown) {
            anchorBootCount == bootCount && elapsedMillis >= anchorElapsedMillis
        } else {
            // Without boot counts, a non-decreasing elapsed clock is the best signal we have.
            elapsedMillis >= anchorElapsedMillis
        }

        return if (sameBoot) {
            // Elapsed realtime cannot be set by the user, so this ignores clock edits entirely.
            anchorWallMillis + (elapsedMillis - anchorElapsedMillis)
        } else {
            // Rebooted: the elapsed delta is meaningless. Accept the clock, but never let it
            // rewind past the anchor — winding back is the cheap way to escape a window.
            maxOf(wallMillis, anchorWallMillis)
        }
    }
}
