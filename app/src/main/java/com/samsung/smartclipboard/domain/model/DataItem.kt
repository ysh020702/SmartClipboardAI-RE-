package com.samsung.smartclipboard.domain.model

data class DataItem(
    val id: Long,
    val type: DataItemType,
    val content: String,
    val title: String? = null,
    val source: String? = null,
    val mimeType: String? = null,
    val createdAt: Long,
    val extractedContent: String? = null,
    /** 이 정보를 찾은 사람이 왜 찾았을까를 나타내는 목적 (문장 형태) */
    val purpose: String? = null,
    /** purpose의 키워드 단위 추출 (콤마 구분), 유사도 분석 및 클러스터링에 사용 */
    val purposeKeyword: String? = null
) {
    /** type이 TEXT면 content를, 아니면 extractedContent(없으면 content fallback)를 반환 */
    val effectiveContent: String
        get() = if (type == DataItemType.TEXT) content else (extractedContent ?: content)
}