package com.samsung.smartclipboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class SuggestedTopic(
    val id: String,
    val title: String,
    val description: String,
    val dataTypes: List<String>,
    val tags: List<String>,
    val icon: ImageVector,
    val color: Color,
    val accentBg: Color,
)

val suggestedTopics = listOf(
    SuggestedTopic("1", "회의 자료", "스크린샷과 메모를 초안, 일정, 알림으로 정리해요.", listOf("스크린샷 3개", "메모 2개"), listOf("업무", "회의"), Icons.Default.Description, AppColors.Blue, AppColors.BlueSoft),
    SuggestedTopic("2", "제주 여행 계획", "숙소, 장소, 링크를 모아 여행 가이드로 정리해요.", listOf("스크린샷 4개", "링크 3개"), listOf("여행", "일정"), Icons.Default.Flight, AppColors.Cyan, Color(0xFFECFEFF)),
    SuggestedTopic("3", "레시피 모음", "저장한 이미지에서 재료와 순서를 뽑아 정리해요.", listOf("스크린샷 6개"), listOf("레시피", "음식"), Icons.Default.CameraAlt, AppColors.Green, Color(0xFFECFDF5)),
    SuggestedTopic("4", "쇼핑 위시리스트", "저장한 상품 링크와 캡처를 비교해요.", listOf("스크린샷 2개", "링크 6개"), listOf("쇼핑"), Icons.Default.ShoppingCart, Color(0xFFD97706), Color(0xFFFFFBEB)),
    SuggestedTopic("5", "개발 참고자료", "코드 캡처와 문서를 주제별로 묶어요.", listOf("스크린샷 5개", "링크 4개"), listOf("개발", "코드"), Icons.Default.Code, Color(0xFF7C3AED), Color(0xFFF5F3FF)),
)

@Composable
fun AiSuggestScreen(navigate: (Screen, Map<String, String>) -> Unit, data: Map<String, String>) {
    val skipLoading = data["skipLoading"] == "true"
    val query = data["query"].orEmpty()
    var loading by remember { mutableStateOf(!skipLoading) }

    LaunchedEffect(skipLoading) {
        if (!skipLoading) {
            delay(1600)
            loading = false
        }
    }

    if (loading) {
        AiSuggestLoading(query = query) { navigate(Screen.Home, emptyMap()) }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGradient)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { navigate(Screen.Home, emptyMap()) },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI 추천 주제", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                ) {
                    Text("분석 결과", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (query.isBlank()) "수집한 전체 데이터에서 ${suggestedTopics.size}개의 주제를 찾았어요."
                        else "\"$query\"에 맞는 주제 ${suggestedTopics.size}개를 찾았어요.",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("하나를 선택하면 AI가 실행 초안을 준비해요.", color = Color(0xFFA5B4FC), fontSize = 10.sp)
                }
            }
        }
        itemsIndexed(suggestedTopics) { _, topic ->
            SuggestedTopicCard(topic = topic) {
                navigate(
                    Screen.TopicDetail,
                    mapOf(
                        "topicId" to topic.id,
                        "topicTitle" to topic.title,
                        "from" to "aiSuggest",
                        "query" to query,
                    ),
                )
            }
        }
        item {
            Button(
                onClick = { navigate(Screen.Data, emptyMap()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppColors.Slate500),
                border = BorderStroke(1.dp, AppColors.Slate200),
            ) {
                Text("데이터 직접 선택", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AiSuggestLoading(query: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = onClose,
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(132.dp))
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(2.dp, Color(0xFF93C5FD).copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF93C5FD), modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("AI가 분석 중입니다", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        if (query.isNotBlank()) {
            Text("\"$query\"", color = Color(0xFF93C5FD), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(28.dp))
        listOf("수집 데이터 확인 중", "패턴 분류 중", "추천 주제 준비 중").forEachIndexed { index, label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
            ) {
                Icon(
                    imageVector = if (index == 0) Icons.Default.Check else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (index == 0) Color(0xFF34D399) else Color(0xFF93C5FD),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(label, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SuggestedTopicCard(topic: SuggestedTopic, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(topic.accentBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(topic.icon, null, tint = topic.color, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(topic.title, color = AppColors.Slate800, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = AppColors.Slate200, modifier = Modifier.size(16.dp))
                    }
                    Text(topic.description, color = AppColors.Slate500, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFBFF))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    topic.dataTypes.forEach { Pill(it, topic.accentBg, topic.color) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    topic.tags.take(2).forEach { Text("#$it", color = AppColors.Slate400, fontSize = 9.sp) }
                }
            }
        }
    }
}
