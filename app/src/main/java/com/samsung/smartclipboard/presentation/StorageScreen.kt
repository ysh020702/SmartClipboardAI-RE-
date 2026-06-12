package com.samsung.smartclipboard.presentation

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.data.source.CollectionPeriod
import com.samsung.smartclipboard.presentation.main.permission.MediaPermissionHelper
import com.samsung.smartclipboard.presentation.main.storage.StorageViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StorageScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
    hasMediaPermission: Boolean = false,
    storageViewModel: StorageViewModel = hiltViewModel(),
) {
    val uiState by storageViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentPermissionState by remember { mutableStateOf(hasMediaPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        currentPermissionState = MediaPermissionHelper.hasImageReadPermission(context)
    }

    val storageOptions = listOf(250 to "250 MB", 500 to "500 MB", 1024 to "1 GB")
    var storageLimit by remember { mutableStateOf(500) }
    val usedStorage = 486.9f
    val percent = (usedStorage / storageLimit).coerceAtMost(1f)

    fun navigateBack() {
        if (data["from"] == "homePanel") {
            navigate(Screen.Home, mapOf("openPanel" to "true"))
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
            .verticalScroll(rememberScrollState())
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
            }
        }

        Spacer(Modifier.height(18.dp))

        // 수집 기간 카드
        CollectionPeriodCard(
            collectionPeriod = uiState.collectionPeriod,
            itemCountInPeriod = uiState.itemCountInPeriod,
            totalItemCount = uiState.totalItemCount,
            onStartClick = { storageViewModel.setStartDate(it) },
            onEndClick = { storageViewModel.setEndDate(it) },
            onClearStart = { storageViewModel.setStartDate(null) },
            onClearEnd = { storageViewModel.setEndDate(null) },
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            hasPermission = currentPermissionState,
            onTogglePermission = { enabled ->
                if (enabled) {
                    permissionLauncher.launch(MediaPermissionHelper.requiredMediaPermissions())
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        setData(Uri.fromParts("package", context.packageName, null))
                    }
                    context.startActivity(intent)
                }
            }
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
private fun CollectionPeriodCard(
    collectionPeriod: CollectionPeriod,
    itemCountInPeriod: Int,
    totalItemCount: Int,
    onStartClick: (Long) -> Unit,
    onEndClick: (Long) -> Unit,
    onClearStart: () -> Unit,
    onClearEnd: () -> Unit,
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

    val startLabel = collectionPeriod.startDateMs?.let { dateFormat.format(it) } ?: "처음부터"
    val endLabel = collectionPeriod.endDateMs?.let { dateFormat.format(it) } ?: "현재까지"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE6EAF2), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("수집 기간", color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("기간 내 데이터 ${itemCountInPeriod}개 / 전체 ${totalItemCount}개", color = Color(0xFF8A94A6), fontSize = 10.sp)
            }
            Box(Modifier.size(34.dp).background(AppColors.BlueSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF1688E8), modifier = Modifier.size(17.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // 시작 날짜
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF7F8FA))
                .border(1.dp, Color(0xFFE6EAF2), RoundedCornerShape(14.dp))
                .clickable {
                    val calendar = Calendar.getInstance()
                    if (collectionPeriod.startDateMs != null) {
                        calendar.timeInMillis = collectionPeriod.startDateMs
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val cal = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                            onStartClick(cal.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF1688E8), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("시작 날짜", color = Color(0xFF8A94A6), fontSize = 9.sp)
                Text(startLabel, color = Color(0xFF111827), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (collectionPeriod.startDateMs != null) {
                Text(
                    "초기화",
                    color = AppColors.Blue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearStart() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 종료 날짜
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF7F8FA))
                .border(1.dp, Color(0xFFE6EAF2), RoundedCornerShape(14.dp))
                .clickable {
                    val calendar = Calendar.getInstance()
                    if (collectionPeriod.endDateMs != null) {
                        calendar.timeInMillis = collectionPeriod.endDateMs
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val cal = Calendar.getInstance().apply { set(year, month, day, 23, 59, 59) }
                            onEndClick(cal.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF1688E8), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("종료 날짜", color = Color(0xFF8A94A6), fontSize = 9.sp)
                Text(endLabel, color = Color(0xFF111827), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (collectionPeriod.endDateMs != null) {
                Text(
                    "초기화",
                    color = AppColors.Blue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearEnd() }
                )
            }
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
private fun PermissionCard(
    hasPermission: Boolean,
    onTogglePermission: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE6EAF2), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasPermission) "권한 허용됨" else "권한 미허용",
                    color = Color(0xFF111827),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    if (hasPermission) "이미지와 스크린샷을 확인할 수 있어요." else "권한을 허용하면 이미지를 확인할 수 있어요.",
                    color = Color(0xFF8A94A6),
                    fontSize = 10.sp,
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .background(AppColors.BlueSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Security,
                    null,
                    tint = Color(0xFF1688E8),
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = hasPermission,
                onCheckedChange = onTogglePermission,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF2563EB),
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE2E8F0),
                    uncheckedThumbColor = Color.White,
                ),
            )
        }
        if (!hasPermission) {
            Spacer(Modifier.height(10.dp))
            Text(
                "권한을 끄려면 시스템 설정에서 변경할 수 있습니다.",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
            )
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
