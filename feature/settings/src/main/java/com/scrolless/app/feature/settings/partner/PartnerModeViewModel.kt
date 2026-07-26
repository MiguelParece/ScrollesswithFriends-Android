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

/**
 * The grantor side: this device holds keys for people it can grant time to (wards).
 */
@HiltViewModel
class PartnerModeViewModel @Inject constructor(private val partnerQuotaManager: PartnerQuotaManager) : ViewModel() {

    val wards: StateFlow<List<PartnerLink>> = partnerQuotaManager.getWards().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Pairing secret shown exactly once after creating a ward identity. */
    private val _oneTimeSecret = MutableStateFlow<String?>(null)
    val oneTimeSecret: StateFlow<String?> = _oneTimeSecret

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode

    fun onCreateWard(name: String) {
        viewModelScope.launch {
            _oneTimeSecret.value = partnerQuotaManager.createWardIdentity(name)
        }
    }

    fun onSecretDismissed() {
        _oneTimeSecret.value = null
    }

    fun onGenerateCode(ward: PartnerLink, challenge: String) {
        viewModelScope.launch {
            _generatedCode.value = partnerQuotaManager.computeCodeForChallenge(ward, challenge)
        }
    }

    fun onCodeDismissed() {
        _generatedCode.value = null
    }

    fun onRevoke(link: PartnerLink) {
        viewModelScope.launch {
            partnerQuotaManager.revokeLink(link)
        }
    }
}
