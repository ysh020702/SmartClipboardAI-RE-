package com.samsung.smartclipboard.presentation.tile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.samsung.smartclipboard.R
import com.samsung.smartclipboard.data.source.clipboard.CaptureResultType
import com.samsung.smartclipboard.data.source.clipboard.ClipboardCaptureResult
import com.samsung.smartclipboard.presentation.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartSaveNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun showCaptureResult(result: ClipboardCaptureResult) {
        Log.d(TAG, "showCaptureResult: type=${result.type}, isSuccess=${result.isSuccess}, message=${result.message}")
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_SMART_SAVE)
            .setSmallIcon(R.drawable.ic_qs_smart_save)
            .setContentTitle(getTitle(result))
            .setContentText(getMessage(result))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
            .apply {
                if (result.isSuccess) {
                    setContentIntent(createOpenAppPendingIntent())
                }
            }
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_SMART_SAVE, notification)
            Log.d(TAG, "showCaptureResult: 알림 표시 완료 (id=$NOTIFICATION_ID_SMART_SAVE)")
        } catch (_: SecurityException) {
            Log.w(TAG, "showCaptureResult: POST_NOTIFICATIONS 권한 없음, 알림 표시 생략")
        }
    }

    private fun getTitle(result: ClipboardCaptureResult): String = when (result.type) {
        CaptureResultType.TEXT_SAVED -> context.getString(R.string.notification_text_saved_title)
        CaptureResultType.LINK_SAVED -> context.getString(R.string.notification_link_saved_title)
        CaptureResultType.EMPTY_CLIPBOARD -> context.getString(R.string.notification_empty_title)
        CaptureResultType.ALREADY_SAVED -> context.getString(R.string.notification_duplicate_title)
        CaptureResultType.SAVE_FAILED -> context.getString(R.string.notification_failed_title)
    }

    private fun getMessage(result: ClipboardCaptureResult): String = when (result.type) {
        CaptureResultType.TEXT_SAVED -> context.getString(R.string.notification_text_saved)
        CaptureResultType.LINK_SAVED -> context.getString(R.string.notification_link_saved)
        CaptureResultType.EMPTY_CLIPBOARD -> context.getString(R.string.notification_empty)
        CaptureResultType.ALREADY_SAVED -> context.getString(R.string.notification_duplicate)
        CaptureResultType.SAVE_FAILED -> context.getString(R.string.notification_failed)
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SMART_SAVE,
                context.getString(R.string.notification_channel_smart_save),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_smart_save_desc)
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "SmartSaveNotificationHelper"
        const val CHANNEL_SMART_SAVE = "smart_save"
        const val NOTIFICATION_ID_SMART_SAVE = 2001
        private const val NOTIFICATION_TIMEOUT_MS = 3000L
    }
}