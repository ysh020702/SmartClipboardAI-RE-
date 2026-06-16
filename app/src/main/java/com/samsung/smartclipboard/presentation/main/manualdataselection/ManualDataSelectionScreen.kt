package com.samsung.smartclipboard.presentation.main.manualdataselection

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.presentation.main.aitopicselection.AnalyzingScreen
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.IconButtonPlain
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManualDataSelectionScreen(
    topicTitle: String,
    navigate: (Screen, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualDataSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = uiState.items
    val selectedIds = uiState.selectedIds

    var aiRecommended by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ManualDataSelectionEffect.NavigateToTopicDetail -> navigate(
                    Screen.TopicDetail,
                    mapOf(
                        "topicId" to effect.topicId.toString(),
                        "topicTitle" to effect.topicTitle,
                        "from" to "topicDataSelect",
                    ),
                )
            }
        }
    }

    // 데이터가 로드되면 AI 추천 한 번 실행
    LaunchedEffect(items.isNotEmpty(), aiRecommended) {
        if (items.isNotEmpty() && !aiRecommended) {
            aiRecommended = true
            viewModel.recommendRelevantItems(topicTitle)
        }
    }

    if (uiState.isCreatingTask || uiState.analysisSteps.isNotEmpty()) {
        AnalyzingScreen(
            navigate = navigate,
            data = mapOf(
                "selectedCount" to selectedIds.size.toString(),
                "topicName" to topicTitle,
            ),
            autoNavigate = false,
            steps = uiState.analysisSteps.ifEmpty { null },
            onGoBack = { viewModel.resetAnalysisSteps() },
        )
        return
    }

    var activeFilter by remember(items) { mutableStateOf(SelectionFilter.All) }
    val filteredItems = remember(activeFilter, items) {
        if (activeFilter == SelectionFilter.All) {
            items
        } else {
            items.filter { it.type.toSelectionFilter() == activeFilter }
        }
    }
    val filterCounts = remember(items) { items.toSelectionFilterCounts() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC)),
    ) {
        TopicDataSelectionHeader(
            topicTitle = topicTitle,
            selectedCount = selectedIds.size,
            isAiRecommending = uiState.isAiRecommending,
            onBack = { navigate(Screen.Home, emptyMap()) },
        )

        Box(
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 14.dp,
                    end = 16.dp,
                    bottom = 92.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SelectionListSummary(
                        itemCount = items.size,
                        selectedCount = selectedIds.size,
                    )
                }

                item {
                    SelectionFilterRow(
                        activeFilter = activeFilter,
                        counts = filterCounts,
                        onSelect = { activeFilter = it },
                    )
                }

                if (uiState.errorMessage != null) {
                    item {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = AppColors.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }

                if (filteredItems.isEmpty()) {
                    item {
                        EmptySelectionData()
                    }
                } else {
                    items(
                        items = filteredItems,
                        key = { it.id },
                    ) { item ->
                        TopicDataItemRow(
                            item = item,
                            selected = item.id in selectedIds,
                            onToggle = {
                                viewModel.toggleSelected(item.id)
                            },
                        )
                    }
                }
            }

            TopicDataSelectionBottomBar(
                selectedCount = selectedIds.size,
                isCreating = uiState.isCreatingTask,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                onCreateTask = {
                    viewModel.createTaskAndAnalyze(topicTitle)
                },
            )
        }
    }
}

@Composable
private fun TopicDataSelectionHeader(
    topicTitle: String,
    selectedCount: Int,
    isAiRecommending: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButtonPlain(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                tint = AppColors.Slate500,
                bg = Color(0xFFF1F5F9),
                onClick = onBack,
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = "분석 데이터 선택",
                color = AppColors.Slate800,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF0F5FF))
                .border(1.dp, Color(0xFFE4EBF7), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AppColors.Blue,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "선택한 주제",
                    color = AppColors.Blue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = topicTitle,
                    color = AppColors.Slate800,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isAiRecommending) {
                        "AI가 관련 데이터를 찾는 중..."
                    } else if (selectedCount > 0) {
                        "AI가 먼저 고른 데이터를 확인하고 조정하세요"
                    } else {
                        "작업에 사용할 데이터를 직접 선택하세요"
                    },
                    color = AppColors.Slate500,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SelectionListSummary(
    itemCount: Int,
    selectedCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "DataItem 목록 · ${itemCount}개",
            color = AppColors.Slate500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, AppColors.Border, CircleShape)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text(
                text = "${selectedCount}개 선택됨",
                color = AppColors.Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SelectionFilterRow(
    activeFilter: SelectionFilter,
    counts: Map<SelectionFilter, Int>,
    onSelect: (SelectionFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SelectionFilter.entries) { filter ->
            val active = activeFilter == filter

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) AppColors.Blue else Color.White)
                    .border(1.dp, if (active) AppColors.Blue else AppColors.Border, CircleShape)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 15.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${filter.label} ${counts[filter] ?: 0}",
                    color = if (active) Color.White else AppColors.Slate500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TopicDataItemRow(
    item: DataItem,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val accent = item.type.toAccentColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.dp,
            color = if (selected) Color(0xFF5B6EF4) else Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TopicDataThumbnail(item = item)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displaySelectionTitle(),
                        color = AppColors.Slate800,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.width(6.dp))

                    Pill(
                        text = item.type.toSelectionLabel(),
                        bg = Color(0xFFF2F5FA),
                        color = AppColors.Slate400,
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text = item.selectionPreviewText(),
                    color = AppColors.Slate500,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = item.createdAt.formatSelectionDate(),
                    color = Color(0xFFB5C0D0),
                    fontSize = 10.sp,
                )
            }

            Spacer(Modifier.width(10.dp))

            SelectionCheck(checked = selected)
        }
    }
}

@Composable
private fun TopicDataThumbnail(
    item: DataItem,
) {
    val imageLike = item.isImageLikeSelectionItem()
    val accent = item.type.toAccentColor()

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(item.type.toSoftColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (imageLike) {
            AsyncImage(
                model = item.content,
                contentDescription = item.displaySelectionTitle(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = item.type.toSelectionIcon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun SelectionCheck(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (checked) AppColors.Blue else Color.White)
            .border(
                width = 2.dp,
                color = if (checked) AppColors.Blue else Color(0xFFCBD5E1),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TopicDataSelectionBottomBar(
    selectedCount: Int,
    isCreating: Boolean,
    modifier: Modifier = Modifier,
    onCreateTask: () -> Unit,
) {
    SelectionCreateTaskButton(
        enabled = selectedCount > 0 && !isCreating,
        modifier = modifier.fillMaxWidth(),
        onClick = onCreateTask,
        text = if (isCreating) "실행 항목 생성 중..." else "선택한 데이터로 작업 생성",
    )
}

@Composable
private fun SelectionCreateTaskButton(
    enabled: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppColors.Blue else Color(0xFFCBD5E1))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun EmptySelectionData() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = AppColors.Slate200,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "선택할 데이터가 없습니다",
            color = AppColors.Slate500,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "먼저 사용할 자료를 수집해 주세요",
            color = AppColors.Slate400,
            fontSize = 11.sp,
        )
    }
}

private fun DataItem.displaySelectionTitle(): String {
    return title
        ?.takeIf { it.isNotBlank() }
        ?: source?.takeIf { it.isNotBlank() }
        ?: content.take(40).ifBlank { "제목 없음" }
}

private fun DataItem.selectionPreviewText(): String {
    return effectiveContent
        .replace("\n", " ")
        .replace("\r", " ")
        .trim()
        .takeIf { it.isNotBlank() }
        ?: "미리볼 수 있는 내용이 없습니다."
}

private fun DataItem.isImageLikeSelectionItem(): Boolean {
    if (type.isImageLikeSelectionType()) return true

    val mime = mimeType.orEmpty().lowercase()
    if (mime.startsWith("image/")) return true

    val raw = content.lowercase()
    return raw.endsWith(".png") ||
        raw.endsWith(".jpg") ||
        raw.endsWith(".jpeg") ||
        raw.endsWith(".webp") ||
        raw.endsWith(".gif")
}

private enum class SelectionFilter(val label: String) {
    All("전체"),
    Text("메모"),
    Link("링크"),
    Image("이미지"),
    File("파일"),
}

private fun List<DataItem>.toSelectionFilterCounts(): Map<SelectionFilter, Int> {
    val counts = mutableMapOf<SelectionFilter, Int>()
    counts[SelectionFilter.All] = size
    forEach { item ->
        val filter = item.type.toSelectionFilter()
        counts[filter] = (counts[filter] ?: 0) + 1
    }
    SelectionFilter.entries.forEach { filter ->
        counts.putIfAbsent(filter, 0)
    }
    return counts
}

private fun DataItemType.toSelectionFilter(): SelectionFilter {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> SelectionFilter.Text
        "LINK", "URL" -> SelectionFilter.Link
        "IMAGE", "PHOTO", "SCREENSHOT", "SCREEN_SHOT" -> SelectionFilter.Image
        "FILE", "DOCUMENT" -> SelectionFilter.File
        else -> SelectionFilter.File
    }
}

private fun DataItemType.toSelectionLabel(): String {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> "메모"
        "LINK", "URL" -> "링크"
        "IMAGE", "PHOTO" -> "이미지"
        "FILE", "DOCUMENT" -> "파일"
        "SCREENSHOT", "SCREEN_SHOT" -> "스크린샷"
        else -> name
    }
}

private fun DataItemType.toSelectionIcon(): ImageVector {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> Icons.AutoMirrored.Filled.Note
        "LINK", "URL" -> Icons.Default.InsertLink
        "IMAGE", "PHOTO" -> Icons.Default.Image
        "FILE", "DOCUMENT" -> Icons.Default.Description
        "SCREENSHOT", "SCREEN_SHOT" -> Icons.Default.CameraAlt
        else -> Icons.Default.Description
    }
}

private fun DataItemType.isImageLikeSelectionType(): Boolean {
    return when (name.uppercase()) {
        "IMAGE", "PHOTO", "SCREENSHOT", "SCREEN_SHOT" -> true
        else -> false
    }
}

private fun DataItemType.toSoftColor(): Color {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> Color(0xFFF8FAFC)
        "LINK", "URL" -> Color(0xFFEFF6FF)
        "IMAGE", "PHOTO" -> Color(0xFFFDF2F8)
        "FILE", "DOCUMENT" -> Color(0xFFF5F3FF)
        "SCREENSHOT", "SCREEN_SHOT" -> Color(0xFFECFEFF)
        else -> Color(0xFFF8FAFC)
    }
}

private fun DataItemType.toAccentColor(): Color {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> AppColors.Slate500
        "LINK", "URL" -> AppColors.Blue
        "IMAGE", "PHOTO" -> Color(0xFFDB2777)
        "FILE", "DOCUMENT" -> Color(0xFF7C3AED)
        "SCREENSHOT", "SCREEN_SHOT" -> Color(0xFF0891B2)
        else -> AppColors.Slate500
    }
}

private fun Long.formatSelectionDate(): String {
    return SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(this))
}

