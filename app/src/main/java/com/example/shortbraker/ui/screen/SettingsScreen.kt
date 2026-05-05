package com.example.shortbraker.ui.screen

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shortbraker.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val limitOptions = listOf(0, 10, 20, 30, 60)

private data class BlockModeOption(
    val mode: String,
    val title: String,
    val description: String
)

private val blockModeOptions = listOf(
    BlockModeOption(
        mode = "NORMAL",
        title = "NORMAL — 홈으로 이동 후 종료",
        description = "제한 초과 시 홈 화면으로 이동하고 유튜브 프로세스를 종료합니다."
    ),
    BlockModeOption(
        mode = "STRICT",
        title = "STRICT — 지정 앱으로 이동 후 종료",
        description = "지정 앱이 없으면 NORMAL과 동일하게 동작합니다."
    )
)

private data class AppOption(
    val packageName: String,
    val name: String,
    val icon: ImageBitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 앱 목록을 백그라운드에서 비동기 로드 (Android 11+ 패키지 가시성 제한 우회)
    var installedApps by remember { mutableStateOf<List<AppOption>>(emptyList()) }
    var appsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    val icon = try {
                        pm.getApplicationIcon(info.activityInfo.packageName)
                            .toBitmap(96, 96).asImageBitmap()
                    } catch (e: Exception) { null }
                    AppOption(info.activityInfo.packageName, info.loadLabel(pm).toString(), icon)
                }
                .sortedBy { it.name }
        }
        installedApps = apps
        appsLoading = false
    }

    val selectedAppName = remember(settings.redirectAppPackage) {
        if (settings.redirectAppPackage.isEmpty()) null
        else try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(settings.redirectAppPackage, 0)).toString()
        } catch (e: Exception) {
            settings.redirectAppPackage
        }
    }

    val selectedAppIcon = remember(settings.redirectAppPackage) {
        if (settings.redirectAppPackage.isEmpty()) null
        else try {
            context.packageManager.getApplicationIcon(settings.redirectAppPackage).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) { null }
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
                            if (minutes == 0) {
                                Column {
                                    Text(
                                        text = "완전 차단",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Shorts 감지 즉시 강제 차단",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(text = "${minutes}분")
                            }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedAppIcon != null) {
                                Image(
                                    bitmap = selectedAppIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Text(
                                text = selectedAppName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showAppPicker = true }) {
                                Text("앱 변경")
                            }
                            OutlinedButton(onClick = { settingsViewModel.updateRedirectApp("") }) {
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

            // 유튜브 앱 전체 차단
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "유튜브 앱 차단",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (settings.blockYoutubeApp) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "유튜브를 열면 즉시 차단 모드 적용",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.blockYoutubeApp,
                            onCheckedChange = { settingsViewModel.updateBlockYoutubeApp(it) }
                        )
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

    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            isLoading = appsLoading,
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
    isLoading: Boolean,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column {
                Text(
                    text = "앱 선택",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("앱 이름 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()

                if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "앱 목록 불러오는 중...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(app.packageName) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            HorizontalDivider()
                        }
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
