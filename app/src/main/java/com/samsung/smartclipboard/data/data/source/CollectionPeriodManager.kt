package com.samsung.smartclipboard.data.source.period

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionPeriodManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("collection_period", Context.MODE_PRIVATE)

    var startDate: Long?
        get() = prefs.getLong(KEY_START_DATE, -1).takeIf { it != -1L }
        set(value) = prefs.edit().putLong(KEY_START_DATE, value ?: -1L).apply()

    var endDate: Long?
        get() = prefs.getLong(KEY_END_DATE, -1).takeIf { it != -1L }
        set(value) = prefs.edit().putLong(KEY_END_DATE, value ?: -1L).apply()

    val isPeriodSet: Boolean
        get() = startDate != null || endDate != null

    var dataLoaded: Boolean
        get() = prefs.getBoolean(KEY_DATA_LOADED, false)
        set(value) = prefs.edit().putBoolean(KEY_DATA_LOADED, value).apply()

    fun clearPeriod() {
        prefs.edit().remove(KEY_START_DATE).remove(KEY_END_DATE).apply()
    }

    fun markDataLoaded() {
        prefs.edit().putBoolean(KEY_DATA_LOADED, true).apply()
    }

    fun resetDataLoaded() {
        prefs.edit().putBoolean(KEY_DATA_LOADED, false).apply()
    }

    fun isWithinPeriod(timestamp: Long): Boolean {
        val start = startDate
        val end = endDate
        if (start == null && end == null) return true
        if (start != null && timestamp < start) return false
        if (end != null && timestamp > end) return false
        return true
    }

    fun getPeriodDescription(): String {
        val start = startDate
        val end = endDate
        return when {
            start == null && end == null -> "전체 기간"
            start == null -> "~ ${formatDate(end!!)}"
            end == null -> "${formatDate(start)} ~ 현재"
            else -> "${formatDate(start)} ~ ${formatDate(end)}"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA)
        return sdf.format(java.util.Date(timestamp))
    }

    companion object {
        private const val KEY_START_DATE = "start_date"
        private const val KEY_END_DATE = "end_date"
        private const val KEY_DATA_LOADED = "data_loaded"
    }
}