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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scrolless.app.core.partner.RedeemResult
import com.scrolless.app.feature.home.R

/**
 * Bottom sheet for redeeming a gift code by pasting the friend's message. Gift links
 * tapped in a chat app skip this sheet entirely and redeem via deep link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerQuotaRequestSheet(redeemResult: RedeemResult?, isRedeeming: Boolean, onRedeem: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pastedText by remember { mutableStateOf("") }

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

            Text(
                text = stringResource(R.string.partner_quota_sheet_instructions),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.partner_quota_sheet_paste_label)) },
                minLines = 2,
                isError = redeemResult != null && redeemResult != RedeemResult.Granted,
                supportingText = {
                    when (redeemResult) {
                        RedeemResult.Invalid -> Text(
                            text = stringResource(R.string.partner_quota_sheet_invalid),
                            color = MaterialTheme.colorScheme.error,
                        )

                        RedeemResult.AlreadyRedeemed -> Text(
                            text = stringResource(R.string.partner_quota_sheet_already_redeemed),
                            color = MaterialTheme.colorScheme.error,
                        )

                        RedeemResult.Expired -> Text(
                            text = stringResource(R.string.partner_quota_sheet_expired),
                            color = MaterialTheme.colorScheme.error,
                        )

                        else -> Unit
                    }
                },
            )

            Button(
                onClick = { onRedeem(pastedText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRedeeming && pastedText.isNotBlank(),
            ) {
                Text(text = stringResource(R.string.partner_quota_sheet_redeem))
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
