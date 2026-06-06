package com.samsung.smartclipboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(navigate: (Screen, Map<String, String>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp)
            .padding(top = 36.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { settingsOpen = true },
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp)),
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", tint = AppColors.Blue)
                }
                DropdownMenu(
                    expanded = settingsOpen,
                    onDismissRequest = { settingsOpen = false },
                    offset = DpOffset(x = (-124).dp, y = 8.dp),
                    modifier = Modifier
                        .width(176.dp)
                        .background(Color(0xFFF8F3FF)),
                ) {
                    DropdownMenuItem(
                        text = { Text("히스토리", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.History, null, tint = AppColors.Blue) },
                        onClick = {
                            settingsOpen = false
                            navigate(Screen.History, emptyMap())
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("저장공간", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Storage, null, tint = AppColors.Blue) },
                        onClick = {
                            settingsOpen = false
                            navigate(Screen.Storage, emptyMap())
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("수집 데이터", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Storage, null, tint = AppColors.Blue) },
                        onClick = {
                            settingsOpen = false
                            navigate(Screen.Data, emptyMap())
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(42.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AppColors.Blue, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("SmartClipboardAI", color = AppColors.Slate800, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("수집한 정보를 AI가 정리해드려요", color = AppColors.Slate400, fontSize = 12.sp)
        }

        Spacer(Modifier.height(38.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlueGradient, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Text("무엇을 정리할까요?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text("자연스럽게 검색하거나 AI에게 주제를 맡겨보세요.", color = Color(0xFFC7D2FE), fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("회의 메모, 여행 링크, 일정 캡처", color = Color.White.copy(alpha = 0.48f), fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.14f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.14f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { navigate(Screen.AiSuggest, mapOf("query" to query)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppColors.Blue),
                ) {
                    Icon(Icons.Default.Psychology, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI 추천", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { navigate(Screen.Data, emptyMap()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                ) {
                    Text("데이터 선택", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

    }
}
