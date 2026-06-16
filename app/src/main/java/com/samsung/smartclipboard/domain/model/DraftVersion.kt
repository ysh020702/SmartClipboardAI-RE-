package com.samsung.smartclipboard.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * TaskReview 화면에서 버전 히스토리를 관리하기 위한 데이터 클래스.
 * DB에는 JSON 문자열로 직렬화되어 저장된다.
 */
data class DraftVersion(
    val version: Int,
    val parentVersion: Int,
    val title: String,
    val body: String,
    val description: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("version", version)
            put("parentVersion", parentVersion)
            put("title", title)
            put("body", body)
            put("description", description)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DraftVersion {
            return DraftVersion(
                version = json.getInt("version"),
                parentVersion = json.getInt("parentVersion"),
                title = json.getString("title"),
                body = json.getString("body"),
                description = json.getString("description")
            )
        }

        fun toJsonString(versions: List<DraftVersion>): String {
            val jsonArray = JSONArray()
            versions.forEach { jsonArray.put(it.toJson()) }
            return jsonArray.toString()
        }

        fun fromJsonString(jsonString: String?): List<DraftVersion> {
            if (jsonString.isNullOrBlank()) return emptyList()
            return try {
                val jsonArray = JSONArray(jsonString)
                (0 until jsonArray.length()).map { fromJson(jsonArray.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}