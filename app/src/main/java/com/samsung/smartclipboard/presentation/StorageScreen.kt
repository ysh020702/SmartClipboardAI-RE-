package com.samsung.smartclipboard.presentation

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.smartclipboard.presentation.main.storage.StorageViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StorageScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val storageOptions = listOf(250 to "250 MB", 500 to "500 MB", 1024 to "1 GB")
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

    if (uiState.showStartDatePicker) {
        val calendar = Calendar.getInstance()
        if (uiState.startDate != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = uiState.startDate!! }
            calendar.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.setStartDate(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { viewModel.hideStartDatePicker() }
            setOnDismissListener { viewModel.hideStartDatePicker() }
        }.show()
    }

    if (uiState.showEndDatePicker) {
        val calendar = Calendar.getInstance()
        if (uiState.endDate != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = uiState.endDate!! }
            calendar.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                viewModel.setEndDate(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { viewModel.hideEndDatePicker() }
            setOnDismissListener { viewModel.hideEndDatePicker() }
        }.show()
    }

    BackHandler { navigateBack() }

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
            subtitle = uiState.periodDescription,
            icon = Icons.Default.CalendarMonth,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("시작 날짜", color = Color(0xFF8A94A6), fontSize = 10.sp)
                    Text(
                        text = uiState.startDate?.let { formatDate(it) } ?: "처음부터",
                        color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    if (uiState.startDate != null) {
                        Button(
                            onClick = { viewModel.setStartDate(null) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("초기화", fontSize = 9.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Button(
                        onClick = { viewModel.showStartDatePicker() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7F5FE), contentColor = Color(0xFF1688E8)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("종료 날짜", color = Color(0xFF8A94A6), fontSize = 10.sp)
                    Text(
                        text = uiState.endDate?.let { formatDate(it) } ?: "현재까지",
                        color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    if (uiState.endDate != null) {
                        Button(
                            onClick = { viewModel.setEndDate(null) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("초기화", fontSize = 9.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Button(
                        onClick = { viewModel.showEndDatePicker() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7F5FE), contentColor = Color(0xFF1688E8)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { viewModel.resetAndLoadData() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isLoading) Color(0xFFF1F5F9) else Color(0xFFFEF2F2),
                    contentColor = if (uiState.isLoading) Color(0xFF94A3B8) else Color(0xFFDC2626)
                ),
                border = BorderStroke(1.dp, if (uiState.isLoading) Color(0xFFE2E8F0) else Color(0xFFFECACA)),
                contentPadding = PaddingValues(vertical = 10.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    Text("데이터 초기화 중...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("데이터 초기화", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF1F8)),
            ) {
                Box(modifier = Modifier.fillMaxWidth(percent).height(10.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1688E8)))
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
            title = "권한 허용 됨",
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    return sdf.format(java.util.Date(timestamp))
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
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE6EAF2), RoundedCornerShape(18.dp)).padding(14.dp),
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