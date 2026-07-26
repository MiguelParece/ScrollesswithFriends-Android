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
package com.scrolless.app.core.domain.utils

import com.scrolless.app.core.blocking.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(ExperimentalCoroutinesApi::class)
class TestSchedulerTimeProvider(private val scheduler: TestCoroutineScheduler) : TimeProvider {

    /**
     * Simulated wall-clock offset. Changing this models the user adjusting the device clock:
     * the wall clock jumps while elapsed realtime keeps ticking with the scheduler.
     */
    var wallClockOffsetMillis: Long = 0L

    /** Simulated boot count; bump via [simulateReboot]. */
    var bootCountValue: Int = 1

    private var bootBaseMillis: Long = 0L

    override fun currentTimeInMillis() = scheduler.currentTime + wallClockOffsetMillis

    override fun localDateNow(): LocalDate {
        return Instant.ofEpochMilli(currentTimeInMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    override fun localDateTimeNow(): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeInMillis()), ZoneId.systemDefault())
    }

    override fun elapsedRealtimeMillis(): Long = scheduler.currentTime - bootBaseMillis

    override fun bootCount(): Int = bootCountValue

    /** Models a reboot: elapsed realtime restarts from zero and the boot count increments. */
    fun simulateReboot() {
        bootBaseMillis = scheduler.currentTime
        bootCountValue += 1
    }
}
