package com.samsung.smartclipboard.presentation.main

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.presentation.ActionReviewScreen
import com.samsung.smartclipboard.presentation.main.topicaisuggest.TopicAiSuggestScreen
import com.samsung.smartclipboard.presentation.AnalyzingScreen
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.GradientButton
import com.samsung.smartclipboard.presentation.HistoryScreen
import com.samsung.smartclipboard.presentation.main.home.HomeScreen
import com.samsung.smartclipboard.presentation.NavTab
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StorageScreen
import com.samsung.smartclipboard.presentation.TasksScreen
import com.samsung.smartclipboard.presentation.main.topicselection.TopicSelectionScreen
import com.samsung.smartclipboard.presentation.main.data.DataScreen
import com.samsung.smartclipboard.presentation.main.data.TopicDataSelectionScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        uiState = uiState,
        onNavigate = viewModel::navigate,
        onBottomTabSelected = viewModel::onBottomTabSelected,
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
    onBottomTabSelected: (NavTab) -> Unit,
    onDataSelectModeChanged: (Boolean) -> Unit,
    onOpenAnalysisSheet: (Int, String) -> Unit,
    onDismissAnalysisSheet: () -> Unit,
    onSheetTopicNameChanged: (String) -> Unit,
    onConfirmAnalysis: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        bottomBar = {
            BottomNavigation(
                activeTab = uiState.activeTab,
                onSelect = onBottomTabSelected,
            )
        },
        containerColor = AppColors.Surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.Surface),
        ) {
            when (uiState.screen) {
                Screen.Home -> HomeScreen(
                    navigate = onNavigate,
                )

                Screen.Data -> DataScreen(
                    navigate = onNavigate,
                    onSelectModeChange = onDataSelectModeChanged,
                    onOpenSheet = onOpenAnalysisSheet,
                )

                Screen.Tasks -> TasksScreen(
                    navigate = onNavigate,
                )

                Screen.History -> HistoryScreen(
                    navigate = onNavigate,
                )

                Screen.Storage -> StorageScreen(
                    navigate = onNavigate,
                )

                Screen.AiSuggest -> TopicAiSuggestScreen(
                    navigate = onNavigate,
                    data = uiState.navData,
                )

                Screen.Analyzing -> AnalyzingScreen(
                    navigate = onNavigate,
                    data = uiState.navData,
                )

                Screen.TopicDetail -> TopicSelectionScreen(
                    navigate = onNavigate,
                    data = uiState.navData,
                )

                Screen.ActionReview -> ActionReviewScreen(
                    navigate = onNavigate,
                    data = uiState.navData,
                )

                Screen.TopicDataSelect -> TopicDataSelectionScreen(
                    topicTitle = uiState.navData["topic"].orEmpty(),
                    navigate = onNavigate,
                )
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

@Composable
fun BottomNavigation(
    activeTab: NavTab,
    onSelect: (NavTab) -> Unit,
) {
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.98f),
        tonalElevation = 8.dp,
    ) {
        val items = listOf(
            NavTab.Home to ("홈" to Icons.Default.Home),
            NavTab.Data to ("데이터" to Icons.Default.Storage),
            NavTab.Tasks to ("작업" to Icons.Default.Work),
        )

        items.forEach { (tab, pair) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = pair.second,
                        contentDescription = pair.first,
                    )
                },
                label = {
                    Text(
                        text = pair.first,
                        fontSize = 11.sp,
                    )
                },
            )
        }
    }
}