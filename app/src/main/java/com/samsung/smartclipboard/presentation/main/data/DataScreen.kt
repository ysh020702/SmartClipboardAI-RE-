package com.samsung.smartclipboard.presentation.main.data

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.model.LinkMetadata
import com.samsung.smartclipboard.domain.model.LinkMetadataCodec
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.IconBubble
import com.samsung.smartclipboard.presentation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    data: Map<String, String> = emptyMap(),
    dataViewModel: DataViewModel = hiltViewModel(),
    onSelectModeChange: (Boolean) -> Unit,
    onOpenSheet: (Int, String) -> Unit,
    deferLoading: Boolean = false,
    onClose: () -> Unit = {},
) {
    val uiState by dataViewModel.uiState.collectAsStateWithLifecycle()
    var previewItem by remember { mutableStateOf<DataItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun navigateBack() {
        when (data["from"]) {
            "homePanel" -> navigate(Screen.Home, mapOf("openPanel" to "instant"))
            "homeDataPanel" -> onClose()
            else -> navigate(Screen.Home, emptyMap())
        }
    }

    BackHandler(enabled = true) {
        when {
            previewItem != null -> previewItem = null
            uiState.selectMode -> dataViewModel.exitSelectMode()
            else -> navigateBack()
        }
    }

    LaunchedEffect(deferLoading) {
        if (!deferLoading) {
            dataViewModel.observeDataItems()
        }
    }

    // DataScreen 진입 시 자동으로 스크린샷 확인 및 수집
    LaunchedEffect(Unit) {
        dataViewModel.importRecentScreenshots()
    }

    // 수집 결과 Snackbar 표시
    LaunchedEffect(uiState.importResult) {
        val result = uiState.importResult ?: return@LaunchedEffect
        val message = if (result.isSuccess) {
            if (result.importedCount > 0) {
                "새 스크린샷 ${result.importedCount}개를 수집했어요"
            } else {
                "새로운 스크린샷이 없어요"
            }
        } else {
            "스크린샷 확인 실패: ${result.message}"
        }
        kotlinx.coroutines.delay(300)
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short,
        )
        dataViewModel.clearImportResult()
    }

    LaunchedEffect(uiState.selectMode) {
        onSelectModeChange(uiState.selectMode)
    }

    val visibleItems = uiState.items

    val filters = listOf("전체", "메모", "링크", "이미지", "파일", "스크린샷")

    val filteredItems = remember(uiState.activeFilter, visibleItems) {
        if (uiState.activeFilter == "전체") visibleItems
        else visibleItems.filter { it.type.toFilterLabel() == uiState.activeFilter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface),
        ) {
            // 상단 네비게이션 바
            if (uiState.selectMode) {
                DataSelectHeader(
                    selectedCount = uiState.selected.size,
                    onSelectAll = { dataViewModel.selectAll(filteredItems.map { it.id }) },
                    onCancel = { dataViewModel.exitSelectMode() },
                )
            } else {
                DataNormalHeader(
                    itemCount = visibleItems.size,
                    onBack = { navigateBack() },
                )
            }

            // 필터
            FilterRow(
                filters = filters,
                active = uiState.activeFilter,
                countsByFilter = visibleItems.toFilterCounts(),
                onSelect = { dataViewModel.changeFilter(it) },
            )

            if (filteredItems.isEmpty()) {
                EmptyDataState()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val selected = item.id in uiState.selected
                        DataItemCard(
                            item = item,
                            selected = selected,
                            selectMode = uiState.selectMode,
                            onToggle = { dataViewModel.toggleSelected(item.id) },
                            onLongClick = { dataViewModel.enterSelectMode(item.id) },
                            onPreview = { previewItem = item },
                        )
                    }
                }
            }
        }

        // 수집 결과 Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = AppColors.Slate800,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
            )
        }

        // 선택 모드 시 하단 삭제 버튼
        if (uiState.selectMode && uiState.selected.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shadowElevation = 8.dp,
                color = Color.White,
            ) {
                Button(
                    onClick = { dataViewModel.deleteSelectedItems() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.selected.size}개 삭제", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // 미리보기 오버레이
    previewItem?.let { item ->
        PreviewOverlay(
            item = item,
            onClose = { previewItem = null },
        )
    }
}

// === 일반 모드 헤더 ===
@Composable
private fun DataNormalHeader(
    itemCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = AppColors.Slate500),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("수집 데이터", color = AppColors.Slate800, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("수집한 데이터 ${itemCount}개", color = AppColors.Slate400, fontSize = 10.sp)
        }
    }
}

// === 선택 모드 헤더 (히스토리와 동일) ===
@Composable
private fun DataSelectHeader(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BlueSoft)
            .border(1.dp, Color(0xFFBFDBFE))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "전체 선택",
            color = AppColors.Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onSelectAll),
        )
        Text(
            "${selectedCount}개 선택됨",
            modifier = Modifier.weight(1f),
            color = AppColors.Slate800,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.clickable(onClick = onCancel),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Close, null, tint = AppColors.Slate500, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("취소", color = AppColors.Slate500, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// === 필터 행 ===
@Composable
private fun FilterRow(
    filters: List<String>,
    active: String,
    countsByFilter: Map<String, Int>,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(filters) { filter ->
            val selected = active == filter
            val count = countsByFilter[filter] ?: 0
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) AppColors.Blue else Color(0xFFF1F5F9))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$filter $count",
                    color = if (selected) Color.White else AppColors.Slate500,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

// === 데이터 아이템 카드 ===
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DataItemCard(
    item: DataItem,
    selected: Boolean,
    selectMode: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
    onPreview: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectMode) {
                    Modifier.clickable(onClick = onToggle)
                } else {
                    Modifier.combinedClickable(
                        onClick = onPreview,
                        onLongClick = onLongClick,
                    )
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.Blue else AppColors.Border,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 4.dp, top = 3.dp, end = 12.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            item.bookmarkTitle(),
                            color = AppColors.Slate800,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 19.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            item.bookmarkDescription(),
                            color = AppColors.Slate500,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    BookmarkMetaLine(item = item)
                }

                BookmarkPreview(item = item)
            }

            if (selectMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    SelectionIndicator(checked = selected)
                }
            }
        }
    }
}

@Composable
private fun BookmarkMetaLine(item: DataItem) {
    val metaText = item.bookmarkMetaText()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(item.type.toSoftColor(), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.type.toIcon(), null, tint = item.type.toAccentColor(), modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = metaText,
            color = AppColors.Slate400,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BookmarkPreview(item: DataItem) {
    val linkImageUrl = item.linkMetadata()?.imageUrl

    Box(
        modifier = Modifier
            .width(112.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(item.type.toSoftColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (item.isImageLikeItem() || linkImageUrl != null) {
            AsyncImage(
                model = linkImageUrl ?: item.content,
                contentDescription = item.bookmarkTitle(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(AppColors.Slate900.copy(alpha = 0.48f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = item.type.toFilterLabel(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.type.toIcon(), null, tint = item.type.toAccentColor(), modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.bookmarkTypeLabel(),
                    color = item.type.toAccentColor(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val previewMeta = item.source?.takeIf { it.isNotBlank() }
                    ?: item.mimeType?.takeIf { it.isNotBlank() }
                if (previewMeta != null) {
                    Text(
                        text = previewMeta,
                        color = AppColors.Slate400,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// === 선택 인디케이터 ===
@Composable
private fun SelectionIndicator(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(if (checked) AppColors.Blue else Color.White.copy(alpha = 0.85f), CircleShape)
            .border(
                width = 1.dp,
                color = if (checked) AppColors.Blue else AppColors.Border,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// === 빈 상태 ===
@Composable
private fun EmptyDataState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBubble(
            icon = Icons.Default.CameraAlt,
            tint = AppColors.Slate500,
            bg = Color(0xFFF1F5F9),
            size = 58,
        )
        Spacer(Modifier.height(12.dp))
        Text("수집한 데이터가 없어요.", color = AppColors.Slate400, fontSize = 13.sp)
        Text("새 항목을 추가해 보세요.", color = Color(0xFFCBD5E1), fontSize = 11.sp)
    }
}

// === 미리보기 오버레이 ===
@Composable
private fun PreviewOverlay(
    item: DataItem,
    onClose: () -> Unit,
) {
    val label = item.type.toFilterLabel()
    val icon = item.type.toIcon()
    val bgColor = item.type.toSoftColor()
    val iconColor = item.type.toAccentColor()
    val imageLike = item.isImageLikeItem()

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Slate900)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(item.createdAt.formatDate(), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(bgColor),
            ) {
                if (imageLike) {
                    AsyncImage(
                        model = item.content,
                        contentDescription = item.displayTitle(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    IconBubble(
                        icon = icon,
                        tint = iconColor,
                        bg = Color.White.copy(alpha = 0.55f),
                        size = 72,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        item.displayTitle(),
                        color = if (imageLike) Color.White else AppColors.Slate800,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (imageLike) {
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.Slate900.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        } else Modifier,
                    )
                    val metaText = item.mimeType ?: item.source.orEmpty()
                    if (metaText.isNotBlank()) {
                        Text(
                            metaText,
                            color = if (imageLike) Color.White.copy(alpha = 0.75f) else AppColors.Slate500,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (imageLike) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Slate900.copy(alpha = 0.45f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            } else Modifier,
                        )
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(AppColors.Slate900.copy(alpha = 0.78f))
                        .padding(16.dp),
                ) {
                    Text(item.previewText(), color = Color.White, fontSize = 12.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    item.purpose?.let { purpose ->
                        Spacer(Modifier.height(8.dp))
                        Text("목적: $purpose", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// === 유틸 함수 ===

private fun List<DataItem>.toFilterCounts(): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    counts["전체"] = size
    forEach { item ->
        val label = item.type.toFilterLabel()
        counts[label] = (counts[label] ?: 0) + 1
    }
    return counts
}

private fun DataItem.displayTitle(): String {
    return linkMetadata()?.title
        ?: title?.takeIf { it.isNotBlank() }
        ?: source?.takeIf { it.isNotBlank() }
        ?: content.take(40).ifBlank { "제목 없음" }
}

private fun DataItem.previewText(): String {
    LinkMetadataCodec.displayText(extractedContent)?.let { return it }
    return effectiveContent.replace("\n", " ").replace("\r", " ").trim().takeIf { it.isNotBlank() }
        ?: "미리볼 수 있는 내용이 없습니다."
}

private fun DataItem.bookmarkMetaText(): String {
    val reference = source?.takeIf { it.isNotBlank() }
        ?: mimeType?.takeIf { it.isNotBlank() }
        ?: content.takeIf { it.isNotBlank() }

    return listOfNotNull(
        bookmarkTypeLabel(),
        reference,
        createdAt.formatDate(),
    ).joinToString(" · ")
}

private fun DataItem.bookmarkTitle(): String = displayTitle()

private fun DataItem.bookmarkDescription(): String = previewText()

private fun DataItem.bookmarkTypeLabel(): String {
    return type.toFilterLabel()
}

private fun DataItem.linkMetadata() = LinkMetadataCodec.decode(extractedContent)

private fun DataItem.isImageLikeItem(): Boolean {
    if (type.isImageLike()) return true
    val mime = mimeType.orEmpty().lowercase()
    if (mime.startsWith("image/")) return true
    val raw = content.lowercase()
    return raw.endsWith(".png") || raw.endsWith(".jpg") || raw.endsWith(".jpeg") || raw.endsWith(".webp") || raw.endsWith(".gif")
}

private fun DataItemType.toFilterLabel(): String = when (name.uppercase()) {
    "TEXT", "MEMO", "NOTE" -> "메모"
    "LINK", "URL" -> "링크"
    "IMAGE", "PHOTO" -> "이미지"
    "FILE", "DOCUMENT" -> "파일"
    "SCREENSHOT", "SCREEN_SHOT" -> "스크린샷"
    else -> name
}

private fun DataItemType.toIcon(): ImageVector = when (name.uppercase()) {
    "TEXT", "MEMO", "NOTE" -> Icons.Default.Note
    "LINK", "URL" -> Icons.Default.InsertLink
    "IMAGE", "PHOTO" -> Icons.Default.Image
    "FILE", "DOCUMENT" -> Icons.Default.Description
    "SCREENSHOT", "SCREEN_SHOT" -> Icons.Default.CameraAlt
    else -> Icons.Default.Description
}

private fun DataItemType.isImageLike(): Boolean = when (name.uppercase()) {
    "IMAGE", "PHOTO", "SCREENSHOT", "SCREEN_SHOT" -> true
    else -> false
}

private fun DataItemType.toSoftColor(): Color = when (name.uppercase()) {
    "TEXT", "MEMO", "NOTE" -> Color(0xFFF8FAFC)
    "LINK", "URL" -> Color(0xFFEFF6FF)
    "IMAGE", "PHOTO" -> Color(0xFFFDF2F8)
    "FILE", "DOCUMENT" -> Color(0xFFF5F3FF)
    "SCREENSHOT", "SCREEN_SHOT" -> Color(0xFFECFEFF)
    else -> Color(0xFFF8FAFC)
}

private fun DataItemType.toAccentColor(): Color = when (name.uppercase()) {
    "TEXT", "MEMO", "NOTE" -> AppColors.Slate500
    "LINK", "URL" -> AppColors.Blue
    "IMAGE", "PHOTO" -> Color(0xFFDB2777)
    "FILE", "DOCUMENT" -> Color(0xFF7C3AED)
    "SCREENSHOT", "SCREEN_SHOT" -> Color(0xFF0891B2)
    else -> AppColors.Slate500
}

private fun Long.formatDate(): String {
    return SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(this))
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DataItemCardPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DataItemCard(
            item = DataItem(
                id = 1L,
                type = DataItemType.LINK,
                content = "https://www.notion.com/ko",
                title = "나만을 위한 AI 워크스페이스 | Notion (노션)",
                source = "https://www.notion.com/ko",
                mimeType = "text/html",
                createdAt = 1_716_168_000_000L,
                extractedContent = LinkMetadataCodec.encode(
                    LinkMetadata(
                        title = "나만을 위한 AI 워크스페이스 | Notion (노션)",
                        description = "커스텀 에이전트를 구축하고, 모든 앱을 검색하고, 단순 반복 작업을 자동화하세요.",
                        imageUrl = "https://www.notion.com/images/meta/default.png",
                        textContent = "커스텀 에이전트를 구축하고, 모든 앱을 검색하고, 단순 반복 작업을 자동화하세요.",
                    ),
                ),
            ),
            selected = false,
            selectMode = false,
            onToggle = {},
            onLongClick = {},
            onPreview = {},
        )
        DataItemCard(
            item = DataItem(
                id = 2L,
                type = DataItemType.SCREENSHOT,
                content = "content://sample/screenshot.png",
                title = "회의 자료",
                source = "스크린샷",
                mimeType = "image/png",
                createdAt = 1_716_254_400_000L,
                extractedContent = "주간 업무 보고, Q2 목표 달성률 78%, 팀별 현황 요약",
            ),
            selected = true,
            selectMode = true,
            onToggle = {},
            onLongClick = {},
            onPreview = {},
        )
    }
}
