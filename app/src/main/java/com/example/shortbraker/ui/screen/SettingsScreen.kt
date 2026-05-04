package com.example.shortbraker.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shortbraker.viewmodel.SettingsViewModel

private val limitOptions = listOf(10, 20, 30, 60)

private val blockModeOptions = listOf(
    "SOFT" to "SOFT: 알림만",
    "NORMAL" to "NORMAL: 오버레이 차단",
    "STRICT" to "STRICT: 강제 차단"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 하루 제한 시간
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "하루 Shorts 제한 시간",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    limitOptions.forEach { minutes ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsViewModel.updateDailyLimitMinutes(minutes) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.dailyLimitMinutes == minutes,
                                onClick = { settingsViewModel.updateDailyLimitMinutes(minutes) }
                            )
                            Text(text = "${minutes}분")
                        }
                    }
                }
            }

            // 차단 모드
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "차단 모드",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    blockModeOptions.forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsViewModel.updateBlockMode(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.blockMode == mode,
                                onClick = { settingsViewModel.updateBlockMode(mode) }
                            )
                            Text(text = label)
                        }
                    }
                }
            }

            // 취침 모드
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "취침 모드",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = settings.bedtimeModeEnabled,
                            onCheckedChange = { settingsViewModel.updateBedtimeModeEnabled(it) }
                        )
                    }

                    if (settings.bedtimeModeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        BedtimeTimeField(
                            label = "취침 시작 시간 (예: 23:30)",
                            initialValue = settings.bedtimeStart,
                            onValueConfirmed = { settingsViewModel.updateBedtimeStart(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BedtimeTimeField(
                            label = "취침 종료 시간 (예: 07:00)",
                            initialValue = settings.bedtimeEnd,
                            onValueConfirmed = { settingsViewModel.updateBedtimeEnd(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BedtimeTimeField(
    label: String,
    initialValue: String,
    onValueConfirmed: (String) -> Unit
) {
    var text by rememberSaveable(initialValue) { mutableStateOf(initialValue) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueConfirmed(it)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { onValueConfirmed(text) }
        )
    )
}
