package com.samsung.smartclipboard.presentation.main.permission

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CollectionPeriodSetupScreen(
    onStartPeriodSelected: (Long?) -> Unit,
    onEndPeriodSelected: (Long?) -> Unit,
    onComplete: () -> Unit,
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(Color(0xFFEFF6FF)),
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
                text = "수집할 데이터의 기간을 지정할 수 있습니다.\n시작과 종료 날짜를 선택하거나, 건너뛰어도 됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)).padding(16.dp),
            ) {
                Text(text = "시작 날짜", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = startDate?.let { formatDate(it) } ?: "처음부터", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (startDate != null) Color(0xFF0F172A) else Color(0xFF94A3B8))
                    Row {
                        if (startDate != null) {
                            OutlinedButton(onClick = { startDate = null; onStartPeriodSelected(null) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("초기화", fontSize = 10.sp) }
                            Spacer(Modifier.width(6.dp))
                        }
                        Button(onClick = {
                            val calendar = Calendar.getInstance()
                            if (startDate != null) { val cal = Calendar.getInstance().apply { timeInMillis = startDate!! }; calendar.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)) }
                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                val selected = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                                startDate = selected; onStartPeriodSelected(selected)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)).padding(16.dp),
            ) {
                Text(text = "종료 날짜", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = endDate?.let { formatDate(it) } ?: "현재까지", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (endDate != null) Color(0xFF0F172A) else Color(0xFF94A3B8))
                    Row {
                        if (endDate != null) {
                            OutlinedButton(onClick = { endDate = null; onEndPeriodSelected(null) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("초기화", fontSize = 10.sp) }
                            Spacer(Modifier.width(6.dp))
                        }
                        Button(onClick = {
                            val calendar = Calendar.getInstance()
                            if (endDate != null) { val cal = Calendar.getInstance().apply { timeInMillis = endDate!! }; calendar.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)) }
                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                val selected = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                                endDate = selected; onEndPeriodSelected(selected)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(onClick = onComplete, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("시작하기", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = onComplete, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)), modifier = Modifier.fillMaxWidth().height(44.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                Text("건너뛰기 (전체 기간 수집)", fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(text = "나중에 설정 화면에서 변경할 수 있습니다.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    return sdf.format(java.util.Date(timestamp))
}