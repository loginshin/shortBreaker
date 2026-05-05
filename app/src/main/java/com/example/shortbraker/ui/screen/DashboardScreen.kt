package com.example.shortbraker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shortbraker.ui.navigation.Routes
import com.example.shortbraker.viewmodel.DashboardViewModel
import com.example.shortbraker.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val state by dashboardViewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) dashboardViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shorts Breaker") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 오늘 사용량 카드
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "오늘 사용량",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!state.usageStatsGranted) {
                        Text(
                            text = "앱 사용 통계 권한이 필요합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "아래 [권한 설정] 버튼을 눌러 권한을 허용해 주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        UsageRow(
                            label = "오늘 YouTube 사용 시간",
                            value = "${state.youtubeUsageMinutes}분"
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    UsageRow(
                        label = "오늘 Shorts 추정 사용 시간",
                        value = "${state.shortsUsageMinutes}분"
                    )
                    UsageRow(
                        label = "Shorts 진입 횟수",
                        value = "${state.shortsOpenCount}회"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    UsageRow(
                        label = "Shorts 제한 시간",
                        value = "${settings.dailyLimitMinutes}분"
                    )
                }
            }

            // 권한 상태 요약 카드
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "권한 상태",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        name = "앱 사용 통계",
                        granted = state.usageStatsGranted
                    )
                    PermissionStatusRow(
                        name = "접근성 (Shorts 감지)",
                        granted = state.accessibilityGranted
                    )
                    PermissionStatusRow(
                        name = "다른 앱 위에 표시",
                        granted = state.overlayGranted
                    )

                    if (!state.accessibilityGranted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "접근성 권한이 없으면 Shorts 추정 감지가 동작하지 않습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Shorts 감지 디버그 카드
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Shorts 감지 디버그",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val debug = state.debugState
                    DebugRow(
                        label = "현재 Shorts 감지 상태",
                        value = if (debug.isCurrentlyShorts) "감지 중" else "미감지",
                        highlighted = debug.isCurrentlyShorts
                    )
                    DebugRow(
                        label = "마지막 패키지명",
                        value = debug.lastDetectedPackageName.ifEmpty { "-" }
                    )
                    DebugRow(
                        label = "마지막 이벤트 타입",
                        value = debug.lastEventType.ifEmpty { "-" }
                    )
                    DebugRow(
                        label = "confidence",
                        value = if (debug.lastDetectionTimeMillis == 0L) "-"
                                else "%.2f".format(debug.lastConfidence)
                    )
                    DebugRow(
                        label = "감지 근거",
                        value = debug.lastDetectionReasons.ifEmpty { "-" }
                    )
                    DebugRow(
                        label = "마지막 감지 시각",
                        value = formatDebugTime(debug.lastDetectionTimeMillis)
                    )
                    DebugRow(
                        label = "현재 세션 시작 시각",
                        value = formatDebugTime(debug.currentSessionStartedAt)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("설정") }

            OutlinedButton(
                onClick = { dashboardViewModel.refresh() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("디버그 새로고침") }

            OutlinedButton(
                onClick = { navController.navigate(Routes.PERMISSION) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("권한 설정") }

            OutlinedButton(
                onClick = { navController.navigate(Routes.HISTORY) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("사용 기록") }
        }
    }
}

private fun formatDebugTime(millis: Long): String {
    if (millis == 0L) return "-"
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
}

@Composable
private fun UsageRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionStatusRow(name: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (granted) "허용됨" else "허용 필요",
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun DebugRow(label: String, value: String, highlighted: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlighted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
