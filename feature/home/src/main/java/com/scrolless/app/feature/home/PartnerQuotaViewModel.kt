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
package com.scrolless.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.core.partner.GrantResult
import com.scrolless.app.core.partner.PartnerQuotaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Drives the "ask for more time" bottom sheet of the Partner Quota block option.
 */
@HiltViewModel
class PartnerQuotaViewModel @Inject constructor(private val partnerQuotaManager: PartnerQuotaManager) : ViewModel() {

    val partners: StateFlow<List<PartnerLink>> = partnerQuotaManager.getPartners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _challenge = MutableStateFlow<String?>(null)
    val challenge: StateFlow<String?> = _challenge

    private val _grantResult = MutableStateFlow<GrantResult?>(null)
    val grantResult: StateFlow<GrantResult?> = _grantResult

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying

    /** Loads the active challenge or creates one when the sheet opens. */
    fun onSheetOpened() {
        viewModelScope.launch {
            _grantResult.value = null
            _challenge.value = partnerQuotaManager.getActiveChallenge() ?: partnerQuotaManager.createChallenge()
        }
    }

    fun onNewChallenge() {
        viewModelScope.launch {
            _grantResult.value = null
            _challenge.value = partnerQuotaManager.createChallenge()
        }
    }

    fun onSubmitCode(code: String) {
        if (_isVerifying.value) return
        viewModelScope.launch {
            _isVerifying.value = true
            val result = partnerQuotaManager.submitGrantCode(code)
            Timber.d("Grant code submission -> %s", result::class.simpleName)
            _grantResult.value = result
            if (result is GrantResult.ChallengeExpired || result is GrantResult.TooManyAttempts) {
                _challenge.value = null
            }
            _isVerifying.value = false
        }
    }

    fun onResultConsumed() {
        _grantResult.value = null
    }
}
