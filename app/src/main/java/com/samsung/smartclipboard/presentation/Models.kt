package com.samsung.smartclipboard.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen { Home, Data, Tasks, TopicDetail, ActionReview, Storage, History, AiSuggest, Analyzing, TopicDataSelect }
enum class NavTab { Home, Data, Tasks }
enum class PermissionStatus { Unknown, Selecting, Granted, Partial, Denied }

data class ClipboardItem(
    val id: String,
    val name: String,
    val type: String,
    val mime: String,
    val date: String,
    val color: Color,
    val label: String,
    val preview: String,
)

data class Topic(
    val id: String,
    val title: String,
    val items: Int,
    val lastUpdated: String,
    val color: Color,
    val tags: List<String>,
)

data class ActionConfig(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val app: String,
    val appBg: Color,
    val defaultTitle: String,
    val defaultBody: String,
)

data class ChatMessage(val id: String, val role: MessageRole, val text: String)
enum class MessageRole { Ai, User }

val sampleItems = listOf(
    ClipboardItem("1", "회의자료_스크린샷.jpg", "스크린샷", "이미지", "5월 26일 09:12", AppColors.Blue, "회의 자료", "주간 업무 보고, 2분기 목표 달성률 78%, 대비 현황 요약"),
    ClipboardItem("2", "여행계획_스크린샷.jpg", "스크린샷", "이미지", "5월 24일 13:30", AppColors.BlueDeep, "여행 계획", "제주도 3박 4일, 애월 숙소, 렌터카 예약 완료"),
    ClipboardItem("3", "레시피_스크린샷.jpg", "스크린샷", "이미지", "5월 23일 08:00", AppColors.Cyan, "레시피", "된장찌개 재료: 된장, 두부, 호박, 대파"),
    ClipboardItem("4", "행사안내_스크린샷.jpg", "스크린샷", "이미지", "5월 22일 19:55", AppColors.Green, "행사 안내", "사내 워크숍 안내, 5월 30일 오후 2시, 본사 B동 3층"),
    ClipboardItem("5", "화면테스트_스크린샷.jpg", "스크린샷", "이미지", "5월 20일 16:45", AppColors.Blue, "테스트", "화면 구성요소 레이아웃 테스트, 버튼 정렬과 여백 확인"),
)

val actionConfigs = mapOf(
    "note" to ActionConfig(
        key = "note",
        title = "요약 노트 초안",
        icon = Icons.Default.Description,
        color = AppColors.Blue,
        app = "삼성 노트",
        appBg = Color(0xFFFFF9DB),
        defaultTitle = "스크린샷 수집 요약 노트",
        defaultBody = "수집한 스크린샷 분석 결과\n\n" +
            "• 회의 자료: 주간 업무 보고, 2분기 목표 달성률 78%\n" +
            "• 여행 계획: 제주도 3박 4일, 애월 숙소 예약\n" +
            "• 레시피: 된장찌개 재료 목록\n" +
            "• 행사 안내: 워크숍 5월 30일 오후 2시\n" +
            "• 테스트: 화면 구성요소 레이아웃 확인",
    ),
    "calendar" to ActionConfig(
        key = "calendar",
        title = "캘린더 일정 초안",
        icon = Icons.Default.CalendarMonth,
        color = Color(0xFF2563EB),
        app = "삼성 캘린더",
        appBg = AppColors.BlueSoft,
        defaultTitle = "사내 워크숍 - 5월 30일",
        defaultBody = "일정 정보\n\n제목: 사내 워크숍\n날짜: 2026년 5월 30일\n시간: 오후 2:00 ~ 오후 5:00\n장소: 본사 B동 3층 대회의실\n\n추가 일정\n• 제주도 여행: 6월 3일 ~ 6월 6일",
    ),
    "reminder" to ActionConfig(
        key = "reminder",
        title = "리마인더 초안",
        icon = Icons.Default.Notifications,
        color = AppColors.BlueDeep,
        app = "삼성 리마인더",
        appBg = Color(0xFFF5F3FF),
        defaultTitle = "워크숍 준비 및 재료 구매",
        defaultBody = "알림 목록\n\n• 된장찌개 재료 구매 - 5월 28일 오전 11:00\n• 워크숍 참석 확인 메일 발송 - 5월 29일 오전 9:00\n• 제주도 렌터카 최종 확인 - 6월 2일 오전 10:00",
    ),
    "share" to ActionConfig(
        key = "share",
        title = "공유 초안",
        icon = Icons.Default.Share,
        color = AppColors.Slate500,
        app = "공유하기",
        appBg = AppColors.Surface,
        defaultTitle = "스크린샷 모음 공유",
        defaultBody = "최근 수집한 정보를 정리했습니다.\n\n공유 내용을 편집하세요.",
    ),
)
