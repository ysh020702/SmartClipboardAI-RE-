package com.samsung.smartclipboard.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StorageScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
) {
    val periods = listOf("앱 종료 전까지", "1시간", "24시간", "7일", "직접 설정")
    val storageOptions = listOf(250 to "250 MB", 500 to "500 MB", 1024 to "1 GB")
    var selectedPeriod by remember { mutableStateOf(periods.first()) }
    var storageLimit by remember { mutableStateOf(500) }
    val usedStorage = 486.9f
    val percent = (usedStorage / storageLimit).coerceAtMost(1f)

    fun navigateBack() {
        if (data["from"] == "homePanel") {
            navigate(Screen.Home, mapOf("openPanel" to "instant"))
        } else {
            navigate(Screen.Home, emptyMap())
        }
    }

    BackHandler {
        navigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { navigateBack() },
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppColors.Slate800),
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, AppColors.Border),
            ) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("설정", color = Color(0xFF111827), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("수집 범위와 저장공간", color = Color(0xFF8A94A6), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(18.dp))

        SettingsCard(
            title = "수집 기간",
            subtitle = selectedPeriod,
            icon = Icons.Default.Storage,
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { label ->
                    val active = selectedPeriod == label
                    PillButton(label = label, active = active) { selectedPeriod = label }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(
            title = "저장공간",
            subtitle = "19,087개 항목",
            icon = Icons.Default.Storage,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${usedStorage} MB / ${if (storageLimit >= 1024) "1 GB" else "$storageLimit MB"}", color = Color(0xFF111827), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("정리하기", color = AppColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { navigate(Screen.Data, mapOf("mode" to "cleanup")) })
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEAF1F8)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent)
                        .height(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1688E8)),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                storageOptions.forEach { (value, label) ->
                    PillButton(label = label, active = storageLimit == value) {
                        if (value >= usedStorage) storageLimit = value
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard(
            title = "권한 허용됨",
            subtitle = "이미지와 스크린샷을 확인할 수 있어요.",
            icon = Icons.Default.Security,
            trailing = { Icon(Icons.Default.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp)) },
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { navigate(Screen.Data, emptyMap()) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF6FF), contentColor = AppColors.Blue),
            border = BorderStroke(1.dp, Color(0xFFD5E9FF)),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("데이터 관리자 열기", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("저장된 사진, 링크, 메모를 선택하세요.", fontSize = 10.sp, color = Color(0xFF5C8FC0))
            }
            Icon(Icons.Default.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE6EAF2), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color(0xFF8A94A6), fontSize = 10.sp)
            }
            Box(Modifier.size(34.dp).background(AppColors.BlueSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF1688E8), modifier = Modifier.size(17.dp))
            }
            trailing?.invoke()
        }
        if (content != null) {
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun PillButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Color(0xFFE7F5FE) else Color.White,
            contentColor = if (active) Color(0xFF1688E8) else Color(0xFF586273),
        ),
        border = BorderStroke(1.dp, if (active) Color(0xFFB7DDF2) else Color(0xFFE7EAF0)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
