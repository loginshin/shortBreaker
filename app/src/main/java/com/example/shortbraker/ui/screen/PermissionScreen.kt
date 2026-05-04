package com.example.shortbraker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private data class PermissionInfo(val title: String, val description: String)

private val permissions = listOf(
    PermissionInfo(
        title = "앱 사용 통계 권한",
        description = "YouTube 앱 사용 시간을 측정하기 위해 필요합니다."
    ),
    PermissionInfo(
        title = "접근성 권한",
        description = "YouTube Shorts 화면 감지를 위해 필요합니다."
    ),
    PermissionInfo(
        title = "다른 앱 위에 표시 권한",
        description = "제한 시간 초과 시 차단 화면을 표시하기 위해 필요합니다."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("권한 설정") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            permissions.forEach { permission ->
                PermissionCard(permission)
            }
        }
    }
}

@Composable
private fun PermissionCard(permission: PermissionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = permission.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "현재 단계에서는 권한 확인 로직이 아직 구현되지 않았습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "다음 단계에서 실제 권한 확인 및 설정 화면 이동 기능을 구현합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
