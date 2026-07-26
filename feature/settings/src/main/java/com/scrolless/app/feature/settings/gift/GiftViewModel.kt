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
package com.scrolless.app.feature.settings.gift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.partner.PartnerQuotaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Generates "+15 minutes" gift codes to send to a friend.
 */
@HiltViewModel
class GiftViewModel @Inject constructor(private val partnerQuotaManager: PartnerQuotaManager) : ViewModel() {

    private val _giftCode = MutableStateFlow<String?>(null)
    val giftCode: StateFlow<String?> = _giftCode

    fun onCreateGift() {
        viewModelScope.launch {
            _giftCode.value = partnerQuotaManager.createGiftCode()
        }
    }

    fun onDialogDismissed() {
        _giftCode.value = null
    }
}
