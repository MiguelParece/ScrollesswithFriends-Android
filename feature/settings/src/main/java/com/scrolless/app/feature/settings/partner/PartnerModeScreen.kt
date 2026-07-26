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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scrolless.app.core.model.PartnerLink
import com.scrolless.app.feature.settings.R

/**
 * The grantor side: create pairing secrets for people you supervise and answer their
 * time requests with grant codes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerModeScreen(onNavigateBack: () -> Unit, viewModel: PartnerModeViewModel = hiltViewModel()) {
    val wards by viewModel.wards.collectAsState()
    val oneTimeSecret by viewModel.oneTimeSecret.collectAsState()
    val generatedCode by viewModel.generatedCode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var codeTarget by remember { mutableStateOf<PartnerLink?>(null) }
    var revokeCandidate by remember { mutableStateOf<PartnerLink?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.partner_mode_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.partner_mode_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (wards.isEmpty()) {
                Text(
                    text = stringResource(R.string.partner_mode_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            wards.forEach { ward ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ward.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { codeTarget = ward }) {
                            Text(text = stringResource(R.string.partner_mode_generate_code))
                        }
                        TextButton(onClick = { revokeCandidate = ward }) {
                            Text(
                                text = stringResource(R.string.partner_management_remove),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.partner_mode_add_person))
            }
        }
    }

    if (showAddDialog) {
        AddWardDialog(
            onCreate = { name ->
                viewModel.onCreateWard(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    oneTimeSecret?.let { secret ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.partner_mode_secret_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.partner_mode_secret_instructions))
                    Text(
                        text = secret,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.partner_mode_secret_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onSecretDismissed) {
                    Text(stringResource(R.string.partner_mode_secret_done))
                }
            },
        )
    }

    codeTarget?.let { ward ->
        GenerateCodeDialog(
            ward = ward,
            generatedCode = generatedCode,
            onGenerate = { challenge -> viewModel.onGenerateCode(ward, challenge) },
            onDismiss = {
                codeTarget = null
                viewModel.onCodeDismissed()
            },
        )
    }

    revokeCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { revokeCandidate = null },
            title = { Text(stringResource(R.string.partner_management_revoke_title, candidate.name)) },
            text = { Text(stringResource(R.string.partner_mode_revoke_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onRevoke(candidate)
                        revokeCandidate = null
                    },
                ) {
                    Text(stringResource(R.string.partner_management_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeCandidate = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AddWardDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.partner_mode_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.partner_mode_add_instructions))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.partner_management_name_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.partner_mode_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun GenerateCodeDialog(ward: PartnerLink, generatedCode: String?, onGenerate: (String) -> Unit, onDismiss: () -> Unit) {
    var challenge by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.partner_mode_code_title, ward.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.partner_mode_code_instructions))
                OutlinedTextField(
                    value = challenge,
                    onValueChange = { challenge = it },
                    label = { Text(stringResource(R.string.partner_mode_challenge_label)) },
                    singleLine = true,
                )
                if (generatedCode != null) {
                    Text(
                        text = generatedCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.partner_mode_code_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGenerate(challenge) }, enabled = challenge.isNotBlank()) {
                Text(stringResource(R.string.partner_mode_generate_code))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
