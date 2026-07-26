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
package com.scrolless.app.feature.settings.partner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.core.partner.PartnerQuotaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PairingStatus { IDLE, SUCCESS, ERROR }

/**
 * Manages the list of accountability partners who can grant THIS device extra time.
 */
@HiltViewModel
class PartnerManagementViewModel @Inject constructor(private val partnerQuotaManager: PartnerQuotaManager) : ViewModel() {

    val partners: StateFlow<List<PartnerLink>> = partnerQuotaManager.getPartners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _pairingStatus = MutableStateFlow(PairingStatus.IDLE)
    val pairingStatus: StateFlow<PairingStatus> = _pairingStatus

    fun onPairPartner(name: String, typedSecret: String) {
        viewModelScope.launch {
            _pairingStatus.value = try {
                partnerQuotaManager.pairPartner(name, typedSecret)
                PairingStatus.SUCCESS
            } catch (_: IllegalArgumentException) {
                PairingStatus.ERROR
            }
        }
    }

    fun onPairingStatusConsumed() {
        _pairingStatus.value = PairingStatus.IDLE
    }

    fun onRevoke(link: PartnerLink) {
        viewModelScope.launch {
            partnerQuotaManager.revokeLink(link)
        }
    }
}
