package com.samsung.smartclipboard.presentation

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.collections.get

@Composable
fun ActionReviewScreen(navigate: (Screen, Map<String, String>) -> Unit, data: Map<String, String>) {
    val config = actionConfigs[data["actionType"]] ?: actionConfigs.getValue("note")
    val topicId = data["topicId"] ?: "1"
    val topicTitle = data["topicTitle"] ?: "스크린샷 모음"
    val from = data["from"].orEmpty()
    val query = data["query"].orEmpty()
    val backData = mapOf("topicId" to topicId, "topicTitle" to topicTitle, "from" to from, "query" to query)

    var title by remember { mutableStateOf(config.defaultTitle) }
    var body by remember { mutableStateOf(config.defaultBody) }
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var executed by remember { mutableStateOf(false) }
    var version by remember { mutableStateOf(1) }

    if (executed) {
        LaunchedEffect(Unit) {
            delay(1000)
            navigate(Screen.TopicDetail, backData)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(BlueGradient, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("실행 완료", color = AppColors.Slate800, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("상세 화면으로 돌아가는 중입니다.", color = AppColors.Slate400, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(AppColors.Surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { navigate(Screen.TopicDetail, backData) },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = AppColors.Slate500),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(config.color.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(config.icon, null, tint = config.color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(config.title, color = AppColors.Slate800, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("AI 초안 · 실행 전 검토", color = AppColors.Slate400, fontSize = 10.sp)
                }
            }
        }

        if (version > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, config.color.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Replay, null, tint = config.color, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("수정된 버전 $version", color = config.color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Border),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FieldBlock("제목") {
                        if (editing) {
                            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        } else {
                            ReadOnlyBox { Text(title, color = AppColors.Slate800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    FieldBlock("본문") {
                        if (editing) {
                            OutlinedTextField(value = body, onValueChange = { body = it }, modifier = Modifier.fillMaxWidth(), minLines = 7)
                        } else {
                            ReadOnlyBox { Text(body, color = AppColors.Slate800, fontSize = 11.sp, lineHeight = 17.sp) }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, config.color.copy(alpha = 0.24f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(config.color.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(24.dp).background(BlueGradient, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("AI에게 수정 요청", color = config.color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        if (typing) Text("  ...", color = config.color, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("짧게", "요약만", "제목 변경", "번역", "더 친근하게").forEach { suggestion ->
                            Text(
                                suggestion,
                                modifier = Modifier
                                    .background(config.color.copy(alpha = 0.09f), RoundedCornerShape(20.dp))
                                    .clickable(enabled = !typing) {
                                        typing = true
                                        version += 1
                                        body = "$body\n\n[$suggestion 적용됨]"
                                        typing = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                color = config.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFAFA))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("수정 요청을 입력하세요", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        Button(
                            onClick = {
                                if (input.isNotBlank()) {
                                    version += 1
                                    body = "$body\n\n[수정 요청: $input]"
                                    input = ""
                                }
                            },
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (input.isBlank()) AppColors.Slate200 else config.color),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(Icons.Default.Send, null, tint = if (input.isBlank()) AppColors.Slate400 else Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        editing = !editing
                        if (!editing) version += 1
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = config.color),
                    border = BorderStroke(1.dp, config.color.copy(alpha = 0.24f)),
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (editing) "완료" else "편집", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { executed = true },
                    enabled = !editing,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (editing) AppColors.Slate200 else config.color, contentColor = Color.White),
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("실행", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
