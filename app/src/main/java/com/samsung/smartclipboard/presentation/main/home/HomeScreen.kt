package com.samsung.smartclipboard.presentation.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.BlueGradient
import com.samsung.smartclipboard.presentation.Screen

@Composable
fun HomeScreen(navigate: (Screen, Map<String, String>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }

    val trimmedQuery = query.trim()
    val hasQuery = trimmedQuery.isNotEmpty()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
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
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = AppColors.Blue
                    )
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
                        text = {
                            Text(
                                text = "히스토리",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AppColors.Blue
                            )
                        },
                        onClick = {
                            settingsOpen = false
                            navigate(Screen.History, emptyMap())
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "저장공간",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = AppColors.Blue
                            )
                        },
                        onClick = {
                            settingsOpen = false
                            navigate(Screen.Storage, emptyMap())
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "수집 데이터",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = AppColors.Blue
                            )
                        },
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
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AppColors.Blue,
                modifier = Modifier.size(54.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "SmartClipboardAI",
                color = AppColors.Slate800,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "수집한 정보를 AI가 정리해드려요",
                color = AppColors.Slate400,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(38.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlueGradient, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "무엇을 정리할까요?",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "주제가 있다면 직접 입력하고, 없다면 AI에게 추천받아보세요.",
                color = Color(0xFFC7D2FE),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "회의 메모 정리, 여행 계획, 일정 캡처 분석",
                        color = Color.White.copy(alpha = 0.48f),
                        fontSize = 13.sp
                    )
                },
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

            Button(
                onClick = {
                    navigate(
                        Screen.TopicDataSelect,
                        mapOf(
                            "topic" to trimmedQuery,
                            "mode" to "manual"
                        )
                    )
                },
                enabled = hasQuery,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = AppColors.Blue,
                    disabledContainerColor = Color.White.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = "이 주제로 데이터 선택",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    navigate(
                        Screen.AiSuggest,
                        mapOf("mode" to "ai_topic_recommend")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = "AI 주제 추천",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Spacer(
            modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime)
        )
    }
}