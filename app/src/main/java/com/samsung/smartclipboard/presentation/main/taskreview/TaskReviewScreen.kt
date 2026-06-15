package com.samsung.smartclipboard.presentation.main.taskreview

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.BlueGradient
import com.samsung.smartclipboard.presentation.FieldBlock
import com.samsung.smartclipboard.presentation.ReadOnlyBox
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.actionConfigs



@Composable
fun TaskReviewScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String>,
    viewModel: TaskReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(TaskReviewIntent.Initialize(data))
    }

    val backData = mapOf(
        "topicId" to uiState.topicId,
        "topicTitle" to uiState.topicTitle,
        "from" to uiState.from,
        "query" to uiState.query
    )

    ActionReviewScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = { navigate(Screen.TopicDetail, backData) }
    )
}

@Composable
fun ActionReviewScreenContent(
    uiState: TaskReviewUiState,
    onIntent: (TaskReviewIntent) -> Unit,
    onBack: () -> Unit
) {
    // actionConfigs 및 AppColors, FieldBlock 등은 기존 환경에 정의되어 있다고 가정
    val config = actionConfigs[uiState.actionType] ?: actionConfigs.getValue("note")

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
                    onClick = onBack,
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

        uiState.errorMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(
                                TaskReviewIntent.DismissError
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFFFF1F2)
                    ),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFFDA4AF)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFFBE123C),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 버전 히스토리 컨트롤
        if (uiState.history.size > 1) {
            item {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, config.color.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                            .clickable { onIntent(TaskReviewIntent.ToggleVersionMenu(true)) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.History, null, tint = config.color, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        val parentText = if (uiState.parentVersion > 0) " (from ver. ${uiState.parentVersion})" else ""
                        Text(
                            text = "ver. ${uiState.currentVersion}$parentText",
                            color = config.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null, tint = config.color, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = uiState.isVersionMenuExpanded,
                        onDismissRequest = { onIntent(TaskReviewIntent.ToggleVersionMenu(false)) }
                    ) {
                        uiState.history.sortedByDescending { it.version }.forEach { versionItem ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "ver. ${versionItem.version} - ${versionItem.description}",
                                        fontSize = 13.sp,
                                        fontWeight = if (versionItem.version == uiState.currentVersion) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = { onIntent(TaskReviewIntent.RevertToVersion(versionItem.version)) }
                            )
                        }
                    }
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
                        if (uiState.isEditing) {
                            OutlinedTextField(
                                value = uiState.title,
                                onValueChange = { onIntent(TaskReviewIntent.UpdateTitle(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            ReadOnlyBox {
                                Text(
                                    uiState.title,
                                    color = AppColors.Slate800,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    FieldBlock("본문") {
                        if (uiState.isEditing) {
                            OutlinedTextField(
                                value = uiState.body,
                                onValueChange = { onIntent(TaskReviewIntent.UpdateBody(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 7
                            )
                        } else {
                            ReadOnlyBox {
                                Text(
                                    uiState.body,
                                    color = AppColors.Slate800,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
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
                        Text("현재 내용 기준으로 AI에게 수정 요청", color = config.color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        if (uiState.isRefining) Text("  ...", color = config.color, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickRefineTask.values().forEach { suggestion ->
                            Text(
                                suggestion.label,
                                modifier = Modifier
                                    .background(config.color.copy(alpha = 0.09f), RoundedCornerShape(20.dp))
                                    .clickable(enabled = !uiState.isRefining) {
                                        onIntent(TaskReviewIntent.QuickRefine(suggestion))
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
                            value = uiState.refineInput,
                            onValueChange = { onIntent(TaskReviewIntent.RefineFeedbackChanged(it)) },
                            placeholder = { Text("수정 요청을 입력하세요", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !uiState.isRefining,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        Button(
                            onClick = { onIntent(TaskReviewIntent.StartRefinement) },
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = uiState.refineInput.isNotBlank() && !uiState.isRefining,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.refineInput.isBlank()) AppColors.Slate200 else config.color
                            ),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(Icons.Default.Send, null, tint = if (uiState.refineInput.isBlank()) AppColors.Slate400 else Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onIntent(TaskReviewIntent.ToggleEditMode) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = config.color),
                    border = BorderStroke(1.dp, config.color.copy(alpha = 0.24f)),
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (uiState.isEditing) "수정 완료" else "직접 편집", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onIntent(TaskReviewIntent.ConfirmExecution) },
                    enabled = !uiState.isEditing && !uiState.isRefining && !uiState.isExecuting,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            uiState.isEditing -> AppColors.Slate200
                            uiState.isExecuting -> AppColors.Slate400
                            else -> config.color
                        },
                        contentColor = Color.White
                    ),
                ) {
                    if (uiState.isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("실행 중...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("실행", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    onIntent(TaskReviewIntent.SharePdf)
                },
                enabled =
                    !uiState.isEditing &&
                            !uiState.isRefining &&
                            !uiState.isExecuting &&
                            !uiState.isGeneratingPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = config.color,
                    disabledContainerColor =
                        AppColors.Slate200,
                    disabledContentColor =
                        AppColors.Slate400
                ),
                border = BorderStroke(
                    1.dp,
                    config.color.copy(alpha = 0.24f)
                )
            ) {
                if (uiState.isGeneratingPdf) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        color = config.color,
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(7.dp))

                    Text(
                        "PDF 생성 중...",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(Modifier.width(7.dp))

                    Text(
                        "PDF로 공유",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
