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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.smartclipboard.presentation.ActionReviewScreen
import com.samsung.smartclipboard.presentation.AiSuggestScreen
import com.samsung.smartclipboard.presentation.AnalyzingScreen
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.main.data.DataScreen
import com.samsung.smartclipboard.presentation.GradientButton
import com.samsung.smartclipboard.presentation.HistoryScreen
import com.samsung.smartclipboard.presentation.HomeScreen
import com.samsung.smartclipboard.presentation.NavTab
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StorageScreen
import com.samsung.smartclipboard.presentation.TasksScreen
import com.samsung.smartclipboard.presentation.TopicDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    /*
    * 페이지 이동을 Navigate 함수가 충괄하고, 하위 페이지에 전달함
    * 최상위 컴포즈 함수
    * */
    
    
    var activeTab by remember { mutableStateOf(NavTab.Home) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var navData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var dataSelectMode by remember { mutableStateOf(false) }
    var bottomSheetVisible by remember { mutableStateOf(false) }
    var sheetCount by remember { mutableStateOf(0) }
    var sheetTopicName by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun navigate(target: Screen, data: Map<String, String> = emptyMap()) {
        screen = target
        navData = data
        when (target) {
            Screen.Home -> activeTab = NavTab.Home
            Screen.Data -> activeTab = NavTab.Data
            Screen.Tasks -> activeTab = NavTab.Tasks
            else -> Unit
        }
    }

    val showBottomBar = false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigation(activeTab = activeTab) { tab ->
                    when (tab) {
                        NavTab.Home -> navigate(Screen.Home)
                        NavTab.Data -> navigate(Screen.Data)
                        NavTab.Tasks -> navigate(Screen.Tasks)
                    }
                }
            }
        },
        containerColor = AppColors.Surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.Surface),
        ) {
            when (screen) {
                Screen.Home -> HomeScreen(navigate = ::navigate)
                Screen.Data -> DataScreen(
                    navigate = ::navigate,
                    onSelectModeChange = { dataSelectMode = it },
                    onOpenSheet = { count, title ->
                        sheetCount = count
                        sheetTopicName = title
                        bottomSheetVisible = true
                    },
                )
                Screen.Tasks -> TasksScreen(navigate = ::navigate)
                Screen.History -> HistoryScreen(navigate = ::navigate)
                Screen.Storage -> StorageScreen(navigate = ::navigate)
                Screen.AiSuggest -> AiSuggestScreen(navigate = ::navigate, data = navData)
                Screen.Analyzing -> AnalyzingScreen(navigate = ::navigate, data = navData)
                Screen.TopicDetail -> TopicDetailScreen(navigate = ::navigate, data = navData)
                Screen.ActionReview -> ActionReviewScreen(navigate = ::navigate, data = navData)
            }
        }
    }

    if (bottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { bottomSheetVisible = false },
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
                Text("분석 시작", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Slate800)
                Text("${sheetCount}개의 데이터를 AI 에이전트로 분석합니다.", fontSize = 12.sp, color = AppColors.Slate500)
                OutlinedTextField(
                    value = sheetTopicName,
                    onValueChange = { sheetTopicName = it },
                    label = { Text("주제명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                GradientButton(
                    text = "분석",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        bottomSheetVisible = false
                        dataSelectMode = false
                        navigate(
                            Screen.Analyzing,
                            mapOf(
                                "selectedCount" to sheetCount.toString(),
                                "topicName" to sheetTopicName,
                            ),
                        )
                    },
                )
                Button(
                    onClick = { bottomSheetVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Surface, contentColor = AppColors.Slate500),
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
fun BottomNavigation(activeTab: NavTab, onSelect: (NavTab) -> Unit) {
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
                icon = { Icon(pair.second, contentDescription = pair.first) },
                label = { Text(pair.first, fontSize = 11.sp) },
            )
        }
    }
}
