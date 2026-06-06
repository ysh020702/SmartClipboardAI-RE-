package com.samsung.smartclipboard.presentation.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.samsung.smartclipboard.R
import com.samsung.smartclipboard.presentation.clipboard.ClipboardCaptureActivity

class ClipboardCaptureTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.tile_clipboard_capture_label)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()

        try {
            val intent = Intent(this, ClipboardCaptureActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+ uses PendingIntent
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: PendingIntent.CanceledException) {
            fallbackLaunch()
        } catch (e: RuntimeException) {
            fallbackLaunch()
        }
    }

    private fun fallbackLaunch() {
        try {
            val intent = Intent(this, ClipboardCaptureActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not open clipboard capture",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}