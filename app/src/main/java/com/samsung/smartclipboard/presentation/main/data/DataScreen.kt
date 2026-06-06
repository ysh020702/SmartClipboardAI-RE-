package com.samsung.smartclipboard.presentation.main.data

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.ClipboardItem
import com.samsung.smartclipboard.presentation.DangerSmallButton
import com.samsung.smartclipboard.presentation.GradientButton
import com.samsung.smartclipboard.presentation.IconBubble
import com.samsung.smartclipboard.presentation.IconButtonPlain
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.SkeletonLine
import com.samsung.smartclipboard.presentation.SmallOutlineButton
import com.samsung.smartclipboard.presentation.Thumbnail
import com.samsung.smartclipboard.presentation.sampleItems

// 수집 데이터를 확인만 합니다.
// 권한 요청/확인은 MainActivity에서 처리합니다.
@Composable
fun DataScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    onSelectModeChange: (Boolean) -> Unit,
    onOpenSheet: (Int, String) -> Unit,
) {
    var activeFilter by remember { mutableStateOf("전체") }
    var items by remember { mutableStateOf(sampleItems) }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var previewItem by remember { mutableStateOf<ClipboardItem?>(null) }

    val navigateWithData = navigate

    fun navigate(screen: Screen) {
        navigateWithData(screen, emptyMap())
    }

    fun enterSelect() {
        selectMode = true
        selected = emptySet()
        onSelectModeChange(true)
    }

    fun exitSelect() {
        selectMode = false
        selected = emptySet()
        onSelectModeChange(false)
    }

    BackHandler(selectMode) {
        exitSelect()
    }

    LaunchedEffect(selectMode) {
        onSelectModeChange(selectMode)
    }

    val visibleItems = items
    val filters = listOf("전체", "메모", "링크", "이미지", "파일", "스크린샷")

    val filtered = if (activeFilter == "전체") {
        visibleItems
    } else {
        visibleItems.filter { it.type == activeFilter }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DataHeader(
                selectMode = selectMode,
                selectedCount = selected.size,
                visibleCount = visibleItems.size,
                onDeleteAll = {
                    showDeleteConfirm = true
                },
                onDeleteSelected = {
                    items = items.filterNot { it.id in selected }
                    exitSelect()
                },
                onBackHome = {
                    navigate(Screen.Home)
                },
                onToggleSelect = {
                    if (selectMode) {
                        exitSelect()
                    } else {
                        enterSelect()
                    }
                },
            )

            if (showDeleteConfirm) {
                ConfirmBanner(
                    text = "수집한 데이터 ${visibleItems.size}개를 모두 삭제할까요?",
                    onConfirm = {
                        items = emptyList()
                        showDeleteConfirm = false
                    },
                    onCancel = {
                        showDeleteConfirm = false
                    },
                )
            }

            deleteTargetId?.let { id ->
                ConfirmBanner(
                    text = "이 항목을 삭제할까요?",
                    onConfirm = {
                        items = items.filterNot { it.id == id }
                        deleteTargetId = null
                    },
                    onCancel = {
                        deleteTargetId = null
                    },
                )
            }

            if (selectMode) {
                SelectModeBar(
                    selectedCount = selected.size,
                    onSelectAll = {
                        selected = filtered.map { it.id }.toSet()
                    },
                    onClear = {
                        selected = emptySet()
                    },
                )
            }

            FilterRow(
                filters = filters,
                active = activeFilter,
                screenshotCount = visibleItems.size,
            ) {
                activeFilter = it
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyDataState()
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        DataItemCard(
                            item = item,
                            selected = item.id in selected,
                            selectMode = selectMode,
                            onToggle = {
                                if (selectMode) {
                                    selected = if (item.id in selected) {
                                        selected - item.id
                                    } else {
                                        selected + item.id
                                    }
                                }
                            },
                            onPreview = {
                                previewItem = item
                            },
                            onDelete = {
                                deleteTargetId = item.id
                            },
                        )
                    }
                }
            }

            if (selectMode && selected.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White,
                ) {
                    GradientButton(
                        text = "${selected.size}개 선택 완료",
                        icon = Icons.Default.KeyboardArrowRight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        onOpenSheet(selected.size, "수집한 항목 (${selected.size})")
                    }
                }
            }
        }

        previewItem?.let { item ->
            PreviewOverlay(
                item = item,
                onClose = {
                    previewItem = null
                },
            )
        }
    }
}

@Composable
fun DataHeader(
    selectMode: Boolean,
    selectedCount: Int,
    visibleCount: Int,
    onDeleteAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBackHome: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (selectMode) AppColors.BlueSoft else Color.White)
            .border(1.dp, AppColors.Border)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.width(116.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButtonPlain(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = AppColors.Slate500,
                    bg = Color(0xFFF1F5F9),
                    onClick = onBackHome,
                )

                when {
                    !selectMode -> {
                        DangerSmallButton(
                            text = "전체 삭제",
                            onClick = onDeleteAll,
                        )
                    }

                    selectedCount > 0 -> {
                        DangerSmallButton(
                            text = "삭제",
                            onClick = onDeleteSelected,
                        )
                    }
                }
            }

            Text(
                text = if (selectMode) {
                    if (selectedCount > 0) "${selectedCount}개 선택됨" else "항목 선택"
                } else {
                    "수집 데이터"
                },
                modifier = Modifier.weight(1f),
                color = AppColors.Slate800,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Box(
                Modifier.width(116.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                SmallOutlineButton(
                    text = if (selectMode) "취소" else "선택",
                    active = selectMode,
                    onClick = onToggleSelect,
                )
            }
        }

        Text(
            text = if (selectMode) {
                "${visibleCount}개 중 선택 · 원하는 항목을 고르세요"
            } else {
                "${visibleCount}개 전체 · 텍스트 0 · 링크 0 · 이미지 0 · 스크린샷 ${visibleCount}"
            },
            color = AppColors.Slate400,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
        )
    }
}

@Composable
fun ConfirmBanner(
    text: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AppColors.Red,
            modifier = Modifier.size(16.dp),
        )

        Text(
            text = text,
            color = AppColors.Red,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
        )

        DangerSmallButton(
            text = "삭제",
            onClick = onConfirm,
        )

        Spacer(Modifier.width(6.dp))

        SmallOutlineButton(
            text = "취소",
            onClick = onCancel,
        )
    }
}

@Composable
fun SelectModeBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFDBEAFE))
            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "선택한 데이터 ${selectedCount}개",
            color = AppColors.Blue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "모두 선택",
                color = AppColors.Blue,
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onSelectAll),
            )

            Row(
                modifier = Modifier.clickable(onClick = onClear),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    tint = AppColors.Slate500,
                    modifier = Modifier.size(12.dp),
                )

                Text(
                    text = "초기화",
                    color = AppColors.Slate500,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
fun FilterRow(
    filters: List<String>,
    active: String,
    screenshotCount: Int,
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

            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) AppColors.Blue else Color(0xFFF1F5F9))
                    .clickable {
                        onSelect(filter)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (filter == "스크린샷") {
                        "$filter $screenshotCount"
                    } else {
                        filter
                    },
                    color = if (selected) Color.White else AppColors.Slate500,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
fun DataItemCard(
    item: ClipboardItem,
    selected: Boolean,
    selectMode: Boolean,
    onToggle: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = selectMode,
                onClick = onToggle,
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.Blue else AppColors.Border,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            Thumbnail(
                item = item,
                modifier = Modifier.height(88.dp),
                showCheck = selectMode,
                checked = selected,
            )

            IconButtonPlain(
                icon = Icons.Default.CameraAlt,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center),
                onClick = onPreview,
            )

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Slate900.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = item.preview,
                    color = Color.White,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        text = item.type,
                        bg = Color.White,
                        color = AppColors.Blue
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = item.mime,
                        color = AppColors.Slate400,
                        fontSize = 9.sp,
                    )
                }

                Text(
                    text = item.name,
                    color = AppColors.Slate800,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.date,
                    color = AppColors.Slate400,
                    fontSize = 9.sp,
                )
            }

            if (!selectMode) {
                DangerSmallButton(
                    text = "삭제",
                    icon = Icons.Default.Delete,
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
fun EmptyDataState() {
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
            size = 58
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "수집한 데이터가 없어요.",
            color = AppColors.Slate400,
            fontSize = 13.sp,
        )

        Text(
            text = "새 항목을 추가해 보세요.",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
        )
    }
}

@Composable
fun PreviewOverlay(
    item: ClipboardItem,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Slate900)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = item.date,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                )
            }

            IconButtonPlain(
                icon = Icons.Default.Close,
                tint = Color.White,
                onClick = onClose,
            )
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
                    .background(
                        Brush.linearGradient(
                            listOf(
                                item.color.copy(alpha = 0.25f),
                                item.color.copy(alpha = 0.50f),
                            ),
                        ),
                    ),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SkeletonLine(
                            color = item.color,
                            modifier = Modifier
                                .width(100.dp)
                                .height(14.dp),
                        )

                        SkeletonLine(
                            color = item.color,
                            modifier = Modifier
                                .width(64.dp)
                                .height(14.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(5) {
                            SkeletonLine(
                                color = item.color,
                                modifier = Modifier
                                    .fillMaxWidth(if (it % 2 == 0) 1f else 0.75f)
                                    .height(10.dp),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SkeletonLine(
                            color = item.color,
                            modifier = Modifier
                                .width(112.dp)
                                .height(38.dp),
                        )

                        SkeletonLine(
                            color = item.color,
                            modifier = Modifier
                                .width(80.dp)
                                .height(38.dp),
                        )
                    }
                }

                IconBubble(
                    icon = Icons.Default.CameraAlt,
                    tint = item.color,
                    bg = item.color.copy(alpha = 0.18f),
                    size = 68,
                    modifier = Modifier.align(Alignment.Center),
                )

                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(AppColors.Slate900.copy(alpha = 0.78f))
                        .padding(16.dp),
                ) {
                    Text(
                        text = item.preview,
                        color = Color.White,
                        fontSize = 11.sp,
                    )

                    Text(
                        text = item.name,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}