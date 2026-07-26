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

import kotlin.math.abs

/**
 * Compares a stored (wall, elapsedRealtime, bootCount) anchor against current readings to
 * decide whether the wall clock can be trusted since the anchor was taken.
 *
 * Within a single boot the wall clock and elapsed realtime must advance in lockstep; any
 * divergence beyond [DRIFT_TOLERANCE_MILLIS] means the wall clock was adjusted. Across a
 * reboot elapsed realtime resets, so a forward wall clock cannot be verified — only a
 * backward one is provably manipulated.
 */
object ClockAnomalyDetector {

    const val DRIFT_TOLERANCE_MILLIS = 90_000L

    enum class Verdict {
        /** Wall clock consistent with elapsed realtime. */
        CONSISTENT,

        /** Reboot happened; wall moved forward but cannot be verified. */
        UNVERIFIED_FORWARD,

        /** Wall clock was manipulated (backward, or drifted beyond tolerance in-boot). */
        SUSPICIOUS,
    }

    fun evaluate(
        anchorWallMillis: Long,
        anchorElapsedMillis: Long,
        anchorBootCount: Int,
        wallMillis: Long,
        elapsedMillis: Long,
        bootCount: Int,
    ): Verdict {
        // No anchor recorded yet — nothing to compare against.
        if (anchorWallMillis <= 0L) return Verdict.CONSISTENT

        val bootKnown = anchorBootCount >= 0 && bootCount >= 0
        val sameBoot = if (bootKnown) {
            anchorBootCount == bootCount && elapsedMillis >= anchorElapsedMillis
        } else {
            // Without boot counts, a non-decreasing elapsed clock is the best signal we have.
            elapsedMillis >= anchorElapsedMillis
        }

        return if (sameBoot) {
            val expectedWall = anchorWallMillis + (elapsedMillis - anchorElapsedMillis)
            if (abs(wallMillis - expectedWall) > DRIFT_TOLERANCE_MILLIS) {
                Verdict.SUSPICIOUS
            } else {
                Verdict.CONSISTENT
            }
        } else {
            if (wallMillis < anchorWallMillis) Verdict.SUSPICIOUS else Verdict.UNVERIFIED_FORWARD
        }
    }
}
