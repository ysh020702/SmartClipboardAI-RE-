package com.samsung.smartclipboard.presentation.clipboard

import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.samsung.smartclipboard.data.source.clipboard.ClipboardCaptureHandler
import com.samsung.smartclipboard.data.source.clipboard.ClipboardCaptureResult
import com.samsung.smartclipboard.data.source.clipboard.CaptureResultType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartSaveActivity : ComponentActivity() {

    @Inject
    lateinit var captureHandler: ClipboardCaptureHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SmartSaveActivity 시작")

        @Suppress("DEPRECATION")
        val resultReceiver = intent?.getParcelableExtra<ResultReceiver>(EXTRA_RESULT_RECEIVER)
        Log.d(TAG, "onCreate: resultReceiver=$resultReceiver")

        lifecycleScope.launch {
            // 포커스 획득 대기
            Log.d(TAG, "onCreate: 포커스 획득 대기 (100ms)")
            delay(100L)

            val result = captureHandler.captureLatestClipboard()
            Log.d(TAG, "onCreate: 캡처 결과 - isSuccess=${result.isSuccess}, type=${result.type}, savedCount=${result.savedCount}, message=${result.message}")

            // 결과를 ResultReceiver로 전달
            resultReceiver?.send(0, result.toBundle())
            Log.d(TAG, "onCreate: ResultReceiver로 결과 전달 완료")

            // 즉시 종료
            Log.d(TAG, "onCreate: Activity 종료")
            finish()
        }
    }

    companion object {
        private const val TAG = "SmartSaveActivity"
        const val EXTRA_RESULT_RECEIVER = "extra_result_receiver"
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_RESULT_TYPE = "extra_result_type"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_SAVED_COUNT = "extra_saved_count"
    }
}

private fun ClipboardCaptureResult.toBundle(): Bundle {
    return Bundle().apply {
        putBoolean(SmartSaveActivity.EXTRA_SUCCESS, isSuccess)
        putString(SmartSaveActivity.EXTRA_RESULT_TYPE, type.name)
        putString(SmartSaveActivity.EXTRA_MESSAGE, message)
        putInt(SmartSaveActivity.EXTRA_SAVED_COUNT, savedCount)
    }
}