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
package com.scrolless.app.feature.home.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolless.app.core.partner.GrantCodeCrypto
import com.scrolless.app.core.partner.GrantResult
import com.scrolless.app.feature.home.PartnerQuotaViewModel
import com.scrolless.app.feature.home.R

/**
 * Bottom sheet of the "ask for more time" flow: shows the current challenge to send to a
 * partner over any channel and verifies the 8-digit code they answer with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerQuotaRequestSheet(onDismiss: () -> Unit, onOpenSettings: () -> Unit, viewModel: PartnerQuotaViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val partners by viewModel.partners.collectAsState()
    val challenge by viewModel.challenge.collectAsState()
    val grantResult by viewModel.grantResult.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.onSheetOpened()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.partner_quota_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            if (partners.isEmpty()) {
                Text(
                    text = stringResource(R.string.partner_quota_sheet_no_partners),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.partner_quota_sheet_open_settings))
                }
                return@Column
            }

            when (val result = grantResult) {
                is GrantResult.Granted -> {
                    Text(
                        text = stringResource(R.string.partner_quota_sheet_granted, result.partnerName),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.partner_quota_sheet_done))
                    }
                    return@Column
                }

                else -> Unit
            }

            Text(
                text = stringResource(R.string.partner_quota_sheet_instructions),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = challenge?.let(GrantCodeCrypto::formatGrouped)
                    ?: stringResource(R.string.partner_quota_sheet_challenge_loading),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val current = challenge ?: return@OutlinedButton
                        val text = context.getString(
                            R.string.partner_quota_sheet_share_text,
                            GrantCodeCrypto.formatGrouped(current),
                        )
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.partner_quota_sheet_share))
                }
                OutlinedButton(
                    onClick = {
                        codeInput = ""
                        viewModel.onNewChallenge()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.partner_quota_sheet_new_challenge))
                }
            }

            OutlinedTextField(
                value = codeInput,
                onValueChange = { input -> codeInput = input.filter(Char::isDigit).take(GrantCodeCrypto.CODE_DIGITS) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.partner_quota_sheet_code_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = grantResult is GrantResult.InvalidCode,
                supportingText = {
                    when (val result = grantResult) {
                        is GrantResult.InvalidCode -> Text(
                            text = stringResource(R.string.partner_quota_sheet_invalid_code, result.attemptsRemaining),
                            color = MaterialTheme.colorScheme.error,
                        )

                        GrantResult.ChallengeExpired -> Text(
                            text = stringResource(R.string.partner_quota_sheet_challenge_expired),
                            color = MaterialTheme.colorScheme.error,
                        )

                        GrantResult.TooManyAttempts -> Text(
                            text = stringResource(R.string.partner_quota_sheet_too_many_attempts),
                            color = MaterialTheme.colorScheme.error,
                        )

                        else -> Unit
                    }
                },
            )

            Button(
                onClick = { viewModel.onSubmitCode(codeInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVerifying && codeInput.length == GrantCodeCrypto.CODE_DIGITS && challenge != null,
            ) {
                Text(text = stringResource(R.string.partner_quota_sheet_verify))
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
