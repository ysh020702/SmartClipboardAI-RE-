package com.samsung.smartclipboard.data.source

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.collectionPeriodDataStore by preferencesDataStore(name = "collection_period")

/**
 * 수집 기간 설정을 DataStore에 저장하고 읽어오는 클래스.
 * startDateMs가 null이면 "처음부터", endDateMs가 null이면 "현재까지"를 의미한다.
 */
@Singleton
class CollectionPeriodPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.collectionPeriodDataStore

    companion object {
        val KEY_START_DATE_MS = longPreferencesKey("start_date_ms")
        val KEY_END_DATE_MS = longPreferencesKey("end_date_ms")
        val KEY_ONBOARDING_COMPLETED = longPreferencesKey("onboarding_completed")
    }

    /** 수집 기간 설정을 Flow로 관찰한다. null은 "제한 없음"을 의미한다. */
    val collectionPeriod: Flow<CollectionPeriod> = dataStore.data.map { prefs ->
        CollectionPeriod(
            startDateMs = prefs[KEY_START_DATE_MS],
            endDateMs = prefs[KEY_END_DATE_MS]
        )
    }

    /** 온보딩 완료 여부 */
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] == 1L
    }

    /** 시작 날짜를 설정한다. null이면 "처음부터"를 의미한다. */
    suspend fun setStartDate(dateMs: Long?) {
        dataStore.edit { prefs ->
            if (dateMs != null) prefs[KEY_START_DATE_MS] = dateMs
            else prefs.remove(KEY_START_DATE_MS)
        }
    }

    /** 종료 날짜를 설정한다. null이면 "현재까지"를 의미한다. */
    suspend fun setEndDate(dateMs: Long?) {
        dataStore.edit { prefs ->
            if (dateMs != null) prefs[KEY_END_DATE_MS] = dateMs
            else prefs.remove(KEY_END_DATE_MS)
        }
    }

    /** 온보딩 완료를 표시한다. */
    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = 1L
        }
    }

    /** 수집 기간을 한 번에 설정한다. */
    suspend fun setCollectionPeriod(startDateMs: Long?, endDateMs: Long?) {
        dataStore.edit { prefs ->
            if (startDateMs != null) prefs[KEY_START_DATE_MS] = startDateMs
            else prefs.remove(KEY_START_DATE_MS)
            if (endDateMs != null) prefs[KEY_END_DATE_MS] = endDateMs
            else prefs.remove(KEY_END_DATE_MS)
        }
    }
}

/**
 * 수집 기간 데이터 클래스.
 * startDateMs가 null이면 "처음부터", endDateMs가 null이면 "현재까지"를 의미한다.
 */
data class CollectionPeriod(
    val startDateMs: Long? = null,
    val endDateMs: Long? = null
)