package com.example.shortbraker.ui.screen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shortbraker.viewmodel.SettingsViewModel

private val limitOptions = listOf(10, 20, 30, 60)

private data class BlockModeOption(
    val mode: String,
    val title: String,
    val description: String
)

private val blockModeOptions = listOf(
    BlockModeOption(
        mode = "SOFT",
        title = "SOFT — 알림만",
        description = "제한 초과 시 알림을 표시합니다. (준비 중)"
    ),
    BlockModeOption(
        mode = "NORMAL",
        title = "NORMAL — 오버레이 차단",
        description = "제한 초과 시 화면 위에 차단 카드를 표시합니다."
    ),
    BlockModeOption(
        mode = "STRICT",
        title = "STRICT — 강제 차단",
        description = "차단 카드 표시 후 Shorts 화면에서 자동으로 이탈합니다."
    )
)

private data class AppOption(val packageName: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 설치된 앱 목록 (런처 아이콘이 있는 앱만, 자기 자신 제외)
    val installedApps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { AppOption(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.name }
    }

    // 현재 선택된 앱 이름 (패키지명 → 앱명 변환)
    val selectedAppName = remember(settings.redirectAppPackage) {
        if (settings.redirectAppPackage.isEmpty()) null
        else try {
            val pm = context.packageManager
            pm.getApplicationLabel(
                pm.getApplicationInfo(settings.redirectAppPackage, 0)
            ).toString()
        } catch (e: Exception) {
            settings.redirectAppPackage
        }
    }

    var showAppPicker by remember { mutableStateOf(false) }

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
                    blockModeOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsViewModel.updateBlockMode(option.mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.blockMode == option.mode,
                                onClick = { settingsViewModel.updateBlockMode(option.mode) }
                            )
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 쇼츠 대신 열 앱
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "쇼츠 대신 열 앱",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "제한 시간 초과 시 선택한 앱이 자동으로 실행됩니다. 차단 모드보다 우선 적용됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedAppName != null) {
                        Text(
                            text = "선택된 앱: $selectedAppName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showAppPicker = true }) {
                                Text("앱 변경")
                            }
                            OutlinedButton(
                                onClick = { settingsViewModel.updateRedirectApp("") }
                            ) {
                                Text("선택 해제")
                            }
                        }
                    } else {
                        Text(
                            text = "선택된 앱 없음",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showAppPicker = true }) {
                            Text("앱 선택")
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

    // 앱 선택 다이얼로그
    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            onAppSelected = { packageName ->
                settingsViewModel.updateRedirectApp(packageName)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<AppOption>,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column {
                Text(
                    text = "앱 선택",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(apps) { app ->
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app.packageName) }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                        HorizontalDivider()
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(8.dp)
                ) {
                    Text("취소")
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
    var text by remember(initialValue) { mutableStateOf(initialValue) }

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
