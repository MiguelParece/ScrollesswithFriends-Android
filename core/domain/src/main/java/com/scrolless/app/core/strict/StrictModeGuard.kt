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
package com.scrolless.app.core.strict

import com.scrolless.app.core.model.BlockOption

/**
 * Decides which settings changes are allowed while the strict-mode lock is armed.
 *
 * The rule is one-way: protection may be tightened at any time, never weakened. Every
 * decision here is pure, and callers must consult it before writing to the settings
 * store — disabled controls in the UI are a courtesy, this is the enforcement.
 */
object StrictModeGuard {

    /**
     * How much a block option protects the user, from strongest to weakest. Moving to a
     * lower rank while armed is refused.
     */
    fun strictnessRank(option: BlockOption): Int = when (option) {
        BlockOption.BlockAll -> 4
        BlockOption.PartnerQuota -> 3
        BlockOption.IntervalTimer -> 2
        BlockOption.DailyLimit -> 1
        BlockOption.NothingSelected -> 0
    }

    fun canChangeBlockOption(armed: Boolean, current: BlockOption, next: BlockOption): Boolean =
        !armed || strictnessRank(next) >= strictnessRank(current)

    /** A larger allowance means more watch time, so it is refused while armed. */
    fun canChangeTimeLimit(armed: Boolean, currentMillis: Long, nextMillis: Long): Boolean = !armed || nextMillis <= currentMillis

    /**
     * The interval window is the period the allowance is spread over: a shorter window
     * hands out the allowance more often, so shrinking it is a weakening.
     */
    fun canChangeIntervalConfig(
        armed: Boolean,
        currentAllowanceMillis: Long,
        currentIntervalMillis: Long,
        nextAllowanceMillis: Long,
        nextIntervalMillis: Long,
    ): Boolean = !armed ||
        (nextAllowanceMillis <= currentAllowanceMillis && nextIntervalMillis >= currentIntervalMillis)

    /** Pausing suspends blocking entirely; only cancelling an active pause is allowed. */
    fun canPause(armed: Boolean, shouldPause: Boolean): Boolean = !armed || !shouldPause

    /** Exempting reels sent in DMs removes protection, so it can only be switched off. */
    fun canChangeExceptReelsSentByDm(armed: Boolean, next: Boolean): Boolean = !armed || !next

    /** Feed blocking can only be switched on while armed. */
    fun canChangeInstagramFeedBlocking(armed: Boolean, next: Boolean): Boolean = !armed || next
}
