package com.samsung.smartclipboard.presentation.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.main.taskreview.ActionReviewScreen
import com.samsung.smartclipboard.presentation.main.aitopicselection.TopicAiSuggestScreen
import com.samsung.smartclipboard.presentation.AnalyzingScreen
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.GradientButton
import com.samsung.smartclipboard.presentation.main.history.HistoryScreen
import com.samsung.smartclipboard.presentation.main.home.HomeScreen
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StorageScreen
import com.samsung.smartclipboard.presentation.TasksScreen
import com.samsung.smartclipboard.presentation.main.taskselection.TopicSelectionScreen
import com.samsung.smartclipboard.presentation.main.data.DataScreen
import com.samsung.smartclipboard.presentation.main.home.HomePortalTransition
import com.samsung.smartclipboard.presentation.main.manualdataselection.TopicDataSelectionScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        uiState = uiState,
        onNavigate = viewModel::navigate,
        onDataSelectModeChanged = viewModel::onDataSelectModeChanged,
        onOpenAnalysisSheet = viewModel::openAnalysisSheet,
        onDismissAnalysisSheet = viewModel::dismissAnalysisSheet,
        onSheetTopicNameChanged = viewModel::onSheetTopicNameChanged,
        onConfirmAnalysis = viewModel::confirmAnalysis,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    uiState: MainUiState,
    onNavigate: (Screen, Map<String, String>) -> Unit,
    onDataSelectModeChanged: (Boolean) -> Unit,
    onOpenAnalysisSheet: (Int, String) -> Unit,
    onDismissAnalysisSheet: () -> Unit,
    onSheetTopicNameChanged: (String) -> Unit,
    onConfirmAnalysis: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = AppColors.Surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.Surface),
        ) {
            Crossfade(
                targetState = ScreenRenderState(
                    screen = uiState.screen,
                    navData = uiState.navData,
                ),
                animationSpec = tween(durationMillis = HomePortalTransition.MainScreenCrossfadeMillis),
                label = "main_screen_crossfade",
            ) { target ->
                when (target.screen) {
                    Screen.Home -> HomeScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.Data -> DataScreen(
                        navigate = onNavigate,
                        data = target.navData,
                        onSelectModeChange = onDataSelectModeChanged,
                        onOpenSheet = onOpenAnalysisSheet,
                    )

                    Screen.Tasks -> TasksScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.History -> HistoryScreen(
                        navigate = onNavigate,
                    )

                    Screen.Storage -> StorageScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.AiSuggest -> TopicAiSuggestScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.Analyzing -> AnalyzingScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.TopicDetail -> TopicSelectionScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.ActionReview -> ActionReviewScreen(
                        navigate = onNavigate,
                        data = target.navData,
                    )

                    Screen.TopicDataSelect -> TopicDataSelectionScreen(
                        topicTitle = target.navData["topic"].orEmpty(),
                        navigate = onNavigate,
                    )
                }
            }
        }
    }

    if (uiState.bottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissAnalysisSheet,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "분석 시작",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.Slate800,
                )

                Text(
                    text = "${uiState.sheetCount}개의 데이터를 AI 에이전트로 분석합니다.",
                    fontSize = 12.sp,
                    color = AppColors.Slate500,
                )

                OutlinedTextField(
                    value = uiState.sheetTopicName,
                    onValueChange = onSheetTopicNameChanged,
                    label = { Text("주제명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )

                GradientButton(
                    text = "분석",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onConfirmAnalysis,
                )

                Button(
                    onClick = onDismissAnalysisSheet,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Surface,
                        contentColor = AppColors.Slate500,
                    ),
                    border = BorderStroke(1.dp, AppColors.Slate200),
                ) {
                    Text("취소")
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private data class ScreenRenderState(
    val screen: Screen,
    val navData: Map<String, String>,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenHomePreview() {
    MainScreenContent(
        uiState = MainUiState(
            screen = Screen.Analyzing,
            navData = mapOf(
                "selectedCount" to "3",
                "topicName" to "회의 자료 정리",
            ),
        ),
        onNavigate = { _, _ -> },
        onDataSelectModeChanged = {},
        onOpenAnalysisSheet = { _, _ -> },
        onDismissAnalysisSheet = {},
        onSheetTopicNameChanged = {},
        onConfirmAnalysis = {},
    )
}
