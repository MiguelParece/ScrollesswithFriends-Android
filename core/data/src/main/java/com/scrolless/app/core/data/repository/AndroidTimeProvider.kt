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
package com.scrolless.app.core.data.repository

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.scrolless.app.core.blocking.time.TimeProvider
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * [TimeProvider] backed by the platform clocks, with boot-count support where the API allows.
 */
class AndroidTimeProvider(private val context: Context) : TimeProvider {
    override fun currentTimeInMillis(): Long = System.currentTimeMillis()
    override fun localDateNow(): LocalDate = LocalDate.now()
    override fun localDateTimeNow(): LocalDateTime = LocalDateTime.now()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return -1
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        } catch (e: Settings.SettingNotFoundException) {
            -1
        }
    }
}
