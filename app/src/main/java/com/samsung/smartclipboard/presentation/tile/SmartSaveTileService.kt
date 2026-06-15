package com.samsung.smartclipboard.presentation.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.samsung.smartclipboard.R
import com.samsung.smartclipboard.data.source.clipboard.CaptureResultType
import com.samsung.smartclipboard.data.source.clipboard.ClipboardCaptureResult
import com.samsung.smartclipboard.presentation.clipboard.SmartSaveActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmartSaveTileService : TileService() {

    @Inject
    lateinit var notificationHelper: SmartSaveNotificationHelper

    private val handler = Handler(Looper.getMainLooper())

    private val resultReceiver = object : ResultReceiver(handler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            val isSuccess = resultData?.getBoolean(SmartSaveActivity.EXTRA_SUCCESS, false) ?: false
            val resultTypeStr = resultData?.getString(SmartSaveActivity.EXTRA_RESULT_TYPE)
            val message = resultData?.getString(SmartSaveActivity.EXTRA_MESSAGE)
            val savedCount = resultData?.getInt(SmartSaveActivity.EXTRA_SAVED_COUNT, 0) ?: 0

            Log.d(TAG, "onReceiveResult: isSuccess=$isSuccess, type=$resultTypeStr, savedCount=$savedCount, message=$message")

            val resultType = try {
                CaptureResultType.valueOf(resultTypeStr ?: "")
            } catch (_: Exception) {
                null
            }

            updateTileState(resultType)
            showNotification(isSuccess, message, savedCount, resultType)

            // 2초 후 타일 상태 원래대로 복귀
            handler.postDelayed({ resetTileState() }, TILE_RESET_DELAY_MS)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening: QS 패널 열림, 타일이 보임")
        resetTileState()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick: 타일 탭됨 → SmartSaveActivity 실행 시도")

        // 타일 상태를 "저장 중"으로 변경
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.tile_smart_save_saving)
            updateTile()
        }

        // SmartSaveActivity 실행 (항상 PendingIntent 사용)
        try {
            val intent = Intent(this, SmartSaveActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(SmartSaveActivity.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
            Log.d(TAG, "onClick: startActivityAndCollapse(pendingIntent) 호출 완료")
        } catch (e: Exception) {
            Log.e(TAG, "onClick: PendingIntent 실행 실패, fallback 시도", e)
            fallbackLaunch()
        }
    }

    private fun updateTileState(resultType: CaptureResultType?) {
        Log.d(TAG, "updateTileState: resultType=$resultType")
        qsTile?.apply {
            when (resultType) {
                CaptureResultType.TEXT_SAVED,
                CaptureResultType.LINK_SAVED -> {
                    state = Tile.STATE_ACTIVE
                    label = getString(R.string.tile_smart_save_success)
                }
                CaptureResultType.EMPTY_CLIPBOARD -> {
                    state = Tile.STATE_INACTIVE
                    label = getString(R.string.tile_smart_save_empty)
                }
                CaptureResultType.ALREADY_SAVED -> {
                    state = Tile.STATE_ACTIVE
                    label = getString(R.string.tile_smart_save_duplicate)
                }
                else -> {
                    state = Tile.STATE_INACTIVE
                    label = getString(R.string.tile_smart_save_failed)
                }
            }
            updateTile()
        }
    }

    private fun resetTileState() {
        Log.d(TAG, "resetTileState: 타일 상태 초기화")
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.tile_smart_save_label)
            updateTile()
        }
    }

    private fun showNotification(isSuccess: Boolean, message: String?, savedCount: Int, resultType: CaptureResultType?) {
        Log.d(TAG, "showNotification: isSuccess=$isSuccess, type=$resultType, savedCount=$savedCount")
        val result = ClipboardCaptureResult(
            isSuccess = isSuccess,
            savedCount = savedCount,
            message = message ?: "",
            type = resultType ?: CaptureResultType.SAVE_FAILED
        )
        notificationHelper.showCaptureResult(result)
    }

    private fun fallbackLaunch() {
        Log.d(TAG, "fallbackLaunch: 일반 startActivity로 대체 실행")
        try {
            val intent = Intent(this, SmartSaveActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(SmartSaveActivity.EXTRA_RESULT_RECEIVER, resultReceiver)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not open smart save",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val TAG = "SmartSaveTileService"
        private const val TILE_RESET_DELAY_MS = 2000L
    }
}