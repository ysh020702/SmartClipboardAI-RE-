package com.samsung.smartclipboard.presentation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samsung.smartclipboard.presentation.main.taskreview.TaskReviewScreen
import com.samsung.smartclipboard.presentation.main.aitopicselection.AiTopicSelectionScreen
import com.samsung.smartclipboard.presentation.main.aitopicselection.AnalyzingScreen
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DarkGradient
import com.samsung.smartclipboard.presentation.GradientButton
import com.samsung.smartclipboard.presentation.main.history.HistoryScreen
import com.samsung.smartclipboard.presentation.main.home.HomeScreen
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.StorageScreen
import com.samsung.smartclipboard.presentation.TasksScreen
import com.samsung.smartclipboard.presentation.main.taskselection.TaskSelectionScreen
import com.samsung.smartclipboard.presentation.main.data.DataScreen
import com.samsung.smartclipboard.presentation.main.home.HomePortalTransition
import com.samsung.smartclipboard.presentation.main.manualdataselection.ManualDataSelectionScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    hasMediaPermission: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        uiState = uiState,
        hasMediaPermission = hasMediaPermission,
        onDataSelectModeChanged = viewModel::onDataSelectModeChanged,
        onOpenAnalysisSheet = viewModel::openAnalysisSheet,
        onDismissAnalysisSheet = viewModel::dismissAnalysisSheet,
        onSheetTopicNameChanged = viewModel::onSheetTopicNameChanged,
        onConfirmAnalysis = viewModel::confirmAnalysis,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun MainScreenContent(
    uiState: MainUiState,
    hasMediaPermission: Boolean = false,
    onDataSelectModeChanged: (Boolean) -> Unit,
    onOpenAnalysisSheet: (Int, String) -> Unit,
    onDismissAnalysisSheet: () -> Unit,
    onSheetTopicNameChanged: (String) -> Unit,
    onConfirmAnalysis: () -> Map<String, String>,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navController = rememberNavController()
    val navigate: (Screen, Map<String, String>) -> Unit = { screen, data ->
        navController.navigateTo(screen = screen, data = data)
    }

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkGradient),
        ) {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.routeFor(Screen.Home, emptyMap()),
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    if (isPortalAiSuggestTransition()) {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = HomePortalTransition.MainScreenCrossfadeMillis,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    } else if (isHomePanelStorageTransition()) {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutSlowInEasing,
                            ),
                        ) + slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = FastOutSlowInEasing,
                            ),
                            initialOffsetX = { -it / 12 },
                        )
                    } else if (isStorageHomePanelReturnTransition()) {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    } else {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 280,
                                easing = FastOutSlowInEasing,
                            ),
                        ) + slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = HomePortalTransition.MainScreenCrossfadeMillis,
                                easing = FastOutSlowInEasing,
                            ),
                            initialOffsetX = { it / 5 },
                        )
                    }
                },
                exitTransition = {
                    if (isPortalAiSuggestTransition()) {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = HomePortalTransition.MainScreenCrossfadeMillis,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    } else if (isHomePanelStorageTransition()) {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutSlowInEasing,
                            ),
                        ) + slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutSlowInEasing,
                            ),
                            targetOffsetX = { it / 18 },
                        )
                    } else if (isStorageHomePanelReturnTransition()) {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutSlowInEasing,
                            ),
                        ) + slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutSlowInEasing,
                            ),
                            targetOffsetX = { it / 10 },
                        )
                    } else {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutSlowInEasing,
                            ),
                        ) + slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 280,
                                easing = FastOutSlowInEasing,
                            ),
                            targetOffsetX = { -it / 8 },
                        )
                    }
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 260,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing,
                        ),
                        initialOffsetX = { -it / 8 },
                    )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 260,
                            easing = FastOutSlowInEasing,
                        ),
                        targetOffsetX = { it / 5 },
                    )
                },
            ) {
                MainRoutes.destinations.forEach { destination ->
                    composable(
                        route = destination.routePattern,
                        arguments = listOf(
                            navArgument(MainRoutes.NavDataArg) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { backStackEntry ->
                        val navData = backStackEntry.navData()
                        when (destination.screen) {
                            Screen.Home -> HomeScreen(
                                navigate = navigate,
                                data = navData,
                            )

                            Screen.Data -> DataScreen(
                                navigate = navigate,
                                data = navData,
                                onSelectModeChange = onDataSelectModeChanged,
                                onOpenSheet = onOpenAnalysisSheet,
                            )

                            Screen.Tasks -> TasksScreen(
                                navigate = navigate,
                                data = navData,
                            )

                            Screen.History -> HistoryScreen(
                                navigate = navigate,
                            )

                            Screen.Storage -> StorageScreen(
                                navigate = navigate,
                                data = navData,
                                hasMediaPermission = hasMediaPermission,
                            )

                            Screen.AiSuggest -> AiTopicSelectionScreen(
                                navigate = navigate,
                                data = navData,
                            )

                            Screen.Analyzing -> AnalyzingScreen(
                                navigate = navigate,
                                data = navData,
                            )

                            Screen.TopicDetail -> TaskSelectionScreen(
                                navigate = navigate,
                                data = navData,
                            )

                            Screen.ActionReview -> TaskReviewScreen(
                                navigate = navigate,
                                data = navData,
                            )

                            Screen.TopicDataSelect -> ManualDataSelectionScreen(
                                topicTitle = navData["topic"].orEmpty(),
                                navigate = navigate,
                            )
                        }
                    }
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
                    onClick = {
                        navigate(Screen.Analyzing, onConfirmAnalysis())
                    },
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenHomePreview() {
    MainScreenContent(
        uiState = MainUiState(),
        hasMediaPermission = true,
        onDataSelectModeChanged = {},
        onOpenAnalysisSheet = { _, _ -> },
        onDismissAnalysisSheet = {},
        onSheetTopicNameChanged = {},
        onConfirmAnalysis = { emptyMap() },
    )
}

private fun NavHostController.navigateTo(
    screen: Screen,
    data: Map<String, String>,
) {
    val route = MainRoutes.routeFor(screen, data)
    navigate(route) {
        launchSingleTop = true
        when (screen) {
            Screen.Home -> {
                popUpTo(MainRoutes.routeFor(Screen.Home, emptyMap())) {
                    inclusive = data.isNotEmpty()
                }
            }

            Screen.Data,
            Screen.Tasks -> {
                popUpTo(MainRoutes.routeFor(Screen.Home, emptyMap())) {
                    saveState = true
                }
                restoreState = data.isEmpty()
            }

            Screen.AiSuggest -> {
                popUpTo(MainRoutes.routeFor(Screen.AiSuggest, emptyMap())) {
                    inclusive = false
                }
            }

            else -> Unit
        }
    }
}

private fun NavBackStackEntry.navData(): Map<String, String> {
    return MainRoutes.decodeNavData(arguments?.getString(MainRoutes.NavDataArg))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isPortalAiSuggestTransition(): Boolean {
    return initialState.routeBase() == "home" &&
        targetState.routeBase() == "aiSuggest" &&
        targetState.navData()["mode"] == "ai_topic_recommend"
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isHomePanelStorageTransition(): Boolean {
    return initialState.routeBase() == "home" &&
        targetState.routeBase() == "storage" &&
        targetState.navData()["from"] == "homePanel"
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isStorageHomePanelReturnTransition(): Boolean {
    return initialState.routeBase() == "storage" &&
        targetState.routeBase() == "home" &&
        targetState.navData()["openPanel"] != null
}

private fun NavBackStackEntry.routeBase(): String? {
    return destination.route?.substringBefore("?")
}
