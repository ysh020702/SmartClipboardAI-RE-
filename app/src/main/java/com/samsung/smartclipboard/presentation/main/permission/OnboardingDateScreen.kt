package com.samsung.smartclipboard.presentation.main.permission

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.smartclipboard.presentation.AppColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 앱 최초 실행 시 권한 허용 후 나타나는 수집 기간 설정 화면.
 * 시작 날짜와 종료 날짜를 선택할 수 있으며, 선택하지 않으면 "처음부터 ~ 현재까지"로 설정된다.
 */
@Composable
fun OnboardingDateScreen(
    onComplete: (startMs: Long?, endMs: Long?) -> Unit,
) {
    val context = LocalContext.current
    var startDateMs by remember { mutableStateOf<Long?>(null) }
    var endDateMs by remember { mutableStateOf<Long?>(null) }
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

    val startLabel = startDateMs?.let { dateFormat.format(it) } ?: "처음부터"
    val endLabel = endDateMs?.let { dateFormat.format(it) } ?: "현재까지"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = Color(0xFF2563EB),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "수집 기간을 설정하세요",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "확인할 데이터의 기간을 지정할 수 있습니다.\n설정하지 않으면 모든 기간의 데이터를 확인합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 시작 날짜 선택
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        if (startDateMs != null) {
                            calendar.timeInMillis = startDateMs!!
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val cal = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                                startDateMs = cal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("시작 날짜", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(startLabel, color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (startDateMs != null) {
                    Text(
                        "초기화",
                        color = Color(0xFF2563EB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { startDateMs = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 종료 날짜 선택
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        if (endDateMs != null) {
                            calendar.timeInMillis = endDateMs!!
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val cal = Calendar.getInstance().apply { set(year, month, day, 23, 59, 59) }
                                endDateMs = cal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("종료 날짜", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(endLabel, color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (endDateMs != null) {
                    Text(
                        "초기화",
                        color = Color(0xFF2563EB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { endDateMs = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "선택하지 않으면 처음부터 현재까지 모든 데이터를 확인합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onComplete(startDateMs, endDateMs) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "시작하기",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "나중에 설정에서 변경할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
            )
        }
    }
}