package com.samsung.smartclipboard.data.source.local

import androidx.room.TypeConverter
import com.samsung.smartclipboard.domain.model.InputType

class KeywordConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString("||") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("||")?.filter { it.isNotBlank() } ?: emptyList()

    @TypeConverter
    fun fromInputType(value: InputType): String = value.name

    @TypeConverter
    fun toInputType(value: String): InputType =
        try {
            InputType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            InputType.TEXT
        }
}