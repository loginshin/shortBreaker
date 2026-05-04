package com.example.shortbraker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.shortbraker.ui.navigation.Routes
import com.example.shortbraker.viewmodel.DashboardViewModel
import com.example.shortbraker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()

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
                .padding(16.dp),
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

                    if (!dashboardState.usageStatsGranted) {
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
                            label = "YouTube 사용 시간",
                            value = "${dashboardState.youtubeUsageMinutes}분"
                        )
                        UsageRow(
                            label = "Shorts 사용 시간",
                            value = "0분"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
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
                        granted = dashboardState.usageStatsGranted
                    )
                    PermissionStatusRow(
                        name = "접근성",
                        granted = dashboardState.accessibilityGranted
                    )
                    PermissionStatusRow(
                        name = "다른 앱 위에 표시",
                        granted = dashboardState.overlayGranted
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("설정") }

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
