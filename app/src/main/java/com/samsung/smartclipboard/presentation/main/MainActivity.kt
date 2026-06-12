package com.samsung.smartclipboard.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samsung.smartclipboard.data.source.CollectionPeriodPreferences
import com.samsung.smartclipboard.data.source.screenshot.ScreenshotImportHandler
import com.samsung.smartclipboard.presentation.DarkGradient
import com.samsung.smartclipboard.presentation.SmartClipboardTheme
import com.samsung.smartclipboard.presentation.main.permission.MediaPermissionHelper
import com.samsung.smartclipboard.presentation.main.permission.OnboardingMediaImportResult
import com.samsung.smartclipboard.presentation.main.permission.OnboardingMediaImportScreen
import com.samsung.smartclipboard.presentation.main.permission.OnboardingDateScreen
import com.samsung.smartclipboard.presentation.main.permission.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject

private enum class MainEntryStep {
    Loading,
    Permission,
    MediaImport,
    DateOnboarding,
    Main,
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var collectionPeriodPreferences: CollectionPeriodPreferences

    @Inject
    lateinit var screenshotImportHandler: ScreenshotImportHandler

    private var hasMediaPermission by mutableStateOf(false)
    private var isOnboardingCompleted by mutableStateOf<Boolean?>(null)
    private var permissionSkipped by mutableStateOf<Boolean?>(null)
    private var mediaImportOnboardingCompleted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)
        if (hasMediaPermission) {
            lifecycleScope.launch(Dispatchers.IO) {
                collectionPeriodPreferences.setPermissionSkipped(false)
            }
            permissionSkipped = false
            mediaImportOnboardingCompleted = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)

        // 온보딩 완료 여부 및 권한 건너뛰기 여부 확인
        lifecycleScope.launch {
            collectionPeriodPreferences.isOnboardingCompleted.collect { completed ->
                isOnboardingCompleted = completed
            }
        }
        lifecycleScope.launch {
            collectionPeriodPreferences.isPermissionSkipped.collect { skipped ->
                permissionSkipped = skipped
            }
        }

        setContent {
            SmartClipboardTheme {
                val onboardingCompleted = isOnboardingCompleted
                val skippedPermission = permissionSkipped
                val entryStep = when {
                    onboardingCompleted == null || skippedPermission == null -> MainEntryStep.Loading
                    !hasMediaPermission && !skippedPermission -> MainEntryStep.Permission
                    !onboardingCompleted &&
                        hasMediaPermission &&
                        !mediaImportOnboardingCompleted -> MainEntryStep.MediaImport
                    !onboardingCompleted -> MainEntryStep.DateOnboarding
                    else -> MainEntryStep.Main
                }

                MainEntryContent(
                    entryStep = entryStep,
                    requestMediaPermission = {
                        requestMediaPermission()
                    },
                    skipPermission = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                collectionPeriodPreferences.setPermissionSkipped(true)
                            }
                            permissionSkipped = true
                        }
                    },
                    importScreenshots = { onProgress ->
                        val result = screenshotImportHandler.importRecentScreenshots(
                            onProgress = onProgress,
                        )
                        OnboardingMediaImportResult(
                            isSuccess = result.isSuccess,
                            importedCount = result.importedCount,
                            scannedCount = result.scannedCount,
                            message = result.message,
                        )
                    },
                    completeMediaImport = {
                        mediaImportOnboardingCompleted = true
                    },
                    completeDateOnboarding = { startMs, endMs ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                collectionPeriodPreferences.setCollectionPeriod(startMs, endMs)
                                collectionPeriodPreferences.setOnboardingCompleted()
                            }
                            isOnboardingCompleted = true
                        }
                    },
                    hasMediaPermission = hasMediaPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)
    }

    private fun requestMediaPermission() {
        permissionLauncher.launch(
            MediaPermissionHelper.requiredMediaPermissions()
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MainEntryContent(
    entryStep: MainEntryStep,
    requestMediaPermission: () -> Unit,
    skipPermission: () -> Unit,
    importScreenshots: suspend (suspend (Float) -> Unit) -> OnboardingMediaImportResult,
    completeMediaImport: () -> Unit,
    completeDateOnboarding: (Long?, Long?) -> Unit,
    hasMediaPermission: Boolean,
) {
    AnimatedContent(
        targetState = entryStep,
        transitionSpec = {
            val enter = mainEntryEnterTransition(
                initialStep = initialState,
                targetStep = targetState,
            )
            val exit = mainEntryExitTransition(
                initialStep = initialState,
                targetStep = targetState,
            )
            enter togetherWith exit
        },
        label = "main-entry-content",
    ) { step ->
        when (step) {
            MainEntryStep.Loading -> MainEntryLoadingScreen()

            MainEntryStep.Permission -> PermissionScreen(
                onRequestPermission = requestMediaPermission,
                onSkipPermission = skipPermission,
            )

            MainEntryStep.MediaImport -> OnboardingMediaImportScreen(
                onImportScreenshots = importScreenshots,
                onContinue = completeMediaImport,
            )

            MainEntryStep.DateOnboarding -> OnboardingDateScreen(
                onComplete = completeDateOnboarding,
            )

            MainEntryStep.Main -> MainScreen(hasMediaPermission = hasMediaPermission)
        }
    }
}

private fun mainEntryEnterTransition(
    initialStep: MainEntryStep,
    targetStep: MainEntryStep,
): EnterTransition {
    return when {
        initialStep == MainEntryStep.Permission && targetStep == MainEntryStep.MediaImport -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 520,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        initialStep == MainEntryStep.MediaImport && targetStep == MainEntryStep.DateOnboarding -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 420,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 460,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetY = { it / 8 },
            )
        }

        targetStep == MainEntryStep.Main -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 360,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 380,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetY = { it / 12 },
            )
        }

        else -> fadeIn(
            animationSpec = tween(
                durationMillis = 280,
                easing = FastOutSlowInEasing,
            ),
        )
    }
}

private fun mainEntryExitTransition(
    initialStep: MainEntryStep,
    targetStep: MainEntryStep,
): ExitTransition {
    return when {
        initialStep == MainEntryStep.Permission && targetStep == MainEntryStep.MediaImport -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        initialStep == MainEntryStep.MediaImport && targetStep == MainEntryStep.DateOnboarding -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing,
                ),
                targetOffsetY = { -it / 10 },
            )
        }

        targetStep == MainEntryStep.Main -> {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 240,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing,
                ),
                targetOffsetY = { -it / 14 },
            )
        }

        else -> fadeOut(
            animationSpec = tween(
                durationMillis = 220,
                easing = FastOutSlowInEasing,
            ),
        )
    }
}

@Composable
private fun MainEntryLoadingScreen() {
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(DarkGradient),
    )
}
