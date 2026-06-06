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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Note
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.presentation.AppColors
import com.samsung.smartclipboard.presentation.DangerSmallButton
import com.samsung.smartclipboard.presentation.GradientButton
import com.samsung.smartclipboard.presentation.IconBubble
import com.samsung.smartclipboard.presentation.IconButtonPlain
import com.samsung.smartclipboard.presentation.Pill
import com.samsung.smartclipboard.presentation.Screen
import com.samsung.smartclipboard.presentation.SmallOutlineButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataScreen(
    navigate: (Screen, Map<String, String>) -> Unit,
    dataViewModel: DataViewModel = hiltViewModel(),
    onSelectModeChange: (Boolean) -> Unit,
    onOpenSheet: (Int, String) -> Unit,
) {
    /*
    * 이 화면에 올 때마다, 스크린샷 데이터를 자동으로 연동합니다.
    * 나머지 이미지, 링크 데이터는 사용자가 공유 버튼으로 가져옵니다.
    */

    val uiState by dataViewModel.uiState.collectAsStateWithLifecycle()
    var previewItem by remember { mutableStateOf<DataItem?>(null) }

    fun navigateTo(screen: Screen) {
        navigate(screen, emptyMap())
    }

    BackHandler(uiState.selectMode) {
        dataViewModel.exitSelectMode()
    }

    LaunchedEffect(Unit) {
        dataViewModel.importScreenShot()
        dataViewModel.observeDataItems()
    }

    LaunchedEffect(uiState.selectMode) {
        onSelectModeChange(uiState.selectMode)
    }

    val visibleItems = uiState.items

    val filters = listOf(
        "전체",
        "메모",
        "링크",
        "이미지",
        "파일",
        "스크린샷",
    )

    val filteredItems = remember(
        uiState.activeFilter,
        visibleItems,
    ) {
        if (uiState.activeFilter == "전체") {
            visibleItems
        } else {
            visibleItems.filter { item ->
                item.type.toFilterLabel() == uiState.activeFilter
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            DataHeader(
                selectMode = uiState.selectMode,
                selectedCount = uiState.selected.size,
                visibleItems = visibleItems,
                onDeleteAll = {
                    dataViewModel.showDeleteAllConfirm()
                },
                onDeleteSelected = {
                    dataViewModel.deleteSelectedItems()
                },
                onBackHome = {
                    navigateTo(Screen.Home)
                },
                onToggleSelect = {
                    if (uiState.selectMode) {
                        dataViewModel.exitSelectMode()
                    } else {
                        dataViewModel.enterSelectMode()
                    }
                },
            )

            if (uiState.showDeleteConfirm) {
                ConfirmBanner(
                    text = "수집한 데이터 ${visibleItems.size}개를 모두 삭제할까요?",
                    onConfirm = {
                        dataViewModel.deleteAllItems()
                    },
                    onCancel = {
                        dataViewModel.hideDeleteAllConfirm()
                    },
                )
            }

            uiState.deleteTargetId?.let { id ->
                ConfirmBanner(
                    text = "이 항목을 삭제할까요?",
                    onConfirm = {
                        dataViewModel.deleteItem(id)
                    },
                    onCancel = {
                        dataViewModel.cancelDeleteItem()
                    },
                )
            }

            if (uiState.selectMode) {
                SelectModeBar(
                    selectedCount = uiState.selected.size,
                    onSelectAll = {
                        dataViewModel.selectAll(filteredItems.map { it.id })
                    },
                    onClear = {
                        dataViewModel.clearSelected()
                    },
                )
            }

            FilterRow(
                filters = filters,
                active = uiState.activeFilter,
                countsByFilter = visibleItems.toFilterCounts(),
                onSelect = { filter ->
                    dataViewModel.changeFilter(filter)
                },
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (filteredItems.isEmpty()) {
                    item {
                        EmptyDataState()
                    }
                } else {
                    items(
                        items = filteredItems,
                        key = { it.id },
                    ) { item ->
                        DataItemCard(
                            item = item,
                            selected = item.id in uiState.selected,
                            selectMode = uiState.selectMode,
                            onToggle = {
                                dataViewModel.toggleSelected(item.id)
                            },
                            onPreview = {
                                previewItem = item
                            },
                            onDelete = {
                                dataViewModel.requestDeleteItem(item.id)
                            },
                        )
                    }
                }
            }

            if (uiState.selectMode && uiState.selected.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White,
                ) {
                    GradientButton(
                        text = "${uiState.selected.size}개 선택 완료",
                        icon = Icons.Default.KeyboardArrowRight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        onOpenSheet(
                            uiState.selected.size,
                            "수집한 항목 (${uiState.selected.size})",
                        )
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
    visibleItems: List<DataItem>,
    onDeleteAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBackHome: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    val counts = visibleItems.toFilterCounts()

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
                    !selectMode && visibleItems.isNotEmpty() -> {
                        DangerSmallButton(
                            text = "전체 삭제",
                            onClick = onDeleteAll,
                        )
                    }

                    selectMode && selectedCount > 0 -> {
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
                "${visibleItems.size}개 중 선택 · 원하는 항목을 고르세요"
            } else {
                "${visibleItems.size}개 전체 · " +
                        "메모 ${counts["메모"] ?: 0} · " +
                        "링크 ${counts["링크"] ?: 0} · " +
                        "이미지 ${counts["이미지"] ?: 0} · " +
                        "파일 ${counts["파일"] ?: 0} · " +
                        "스크린샷 ${counts["스크린샷"] ?: 0}"
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
                    .clickable {
                        onSelect(filter)
                    }
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

@Composable
fun DataItemCard(
    item: DataItem,
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
            DataItemThumbnail(
                item = item,
                modifier = Modifier.height(88.dp),
            )

            IconButtonPlain(
                icon = Icons.Default.CameraAlt,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.Center),
                onClick = onPreview,
            )

            if (selectMode) {
                SelectionIndicator(
                    checked = selected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                )
            }

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
                    text = item.previewText(),
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
                        text = item.type.toFilterLabel(),
                        bg = Color.White,
                        color = AppColors.Blue,
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = item.mimeType ?: item.source.orEmpty(),
                        color = AppColors.Slate400,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = item.displayTitle(),
                    color = AppColors.Slate800,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.createdAt.formatDate(),
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
private fun DataItemThumbnail(
    item: DataItem,
    modifier: Modifier = Modifier,
) {
    val label = item.type.toFilterLabel()
    val icon = item.type.toIcon()
    val bgColor = item.type.toSoftColor()
    val iconColor = item.type.toAccentColor()
    val imageLike = item.isImageLikeItem()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (imageLike) {
            AsyncImage(
                model = item.content,
                contentDescription = item.displayTitle(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            IconBubble(
                icon = icon,
                tint = iconColor,
                bg = Color.White.copy(alpha = 0.65f),
                size = 58,
            )
        }

        Text(
            text = label,
            color = if (imageLike) Color.White else iconColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (imageLike) {
                        AppColors.Slate900.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    },
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SelectionIndicator(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) AppColors.Blue else Color.White.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = if (checked) AppColors.Blue else AppColors.Border,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
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
            size = 58,
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
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = item.createdAt.formatDate(),
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
                        text = item.displayTitle(),
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
                        } else {
                            Modifier
                        },
                    )

                    val metaText = item.mimeType ?: item.source.orEmpty()

                    if (metaText.isNotBlank()) {
                        Text(
                            text = metaText,
                            color = if (imageLike) {
                                Color.White.copy(alpha = 0.75f)
                            } else {
                                AppColors.Slate500
                            },
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (imageLike) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Slate900.copy(alpha = 0.45f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            } else {
                                Modifier
                            },
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
                    Text(
                        text = item.previewText(),
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )

                    item.purpose?.let { purpose ->
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "목적: $purpose",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

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
    return title
        ?.takeIf { it.isNotBlank() }
        ?: source?.takeIf { it.isNotBlank() }
        ?: content.take(40).ifBlank { "제목 없음" }
}

private fun DataItem.previewText(): String {
    return effectiveContent
        .replace("\n", " ")
        .replace("\r", " ")
        .trim()
        .takeIf { it.isNotBlank() }
        ?: "미리볼 수 있는 내용이 없습니다."
}

private fun DataItem.isImageLikeItem(): Boolean {
    if (type.isImageLike()) return true

    val mime = mimeType.orEmpty().lowercase()
    if (mime.startsWith("image/")) return true

    val raw = content.lowercase()
    return raw.endsWith(".png") ||
            raw.endsWith(".jpg") ||
            raw.endsWith(".jpeg") ||
            raw.endsWith(".webp") ||
            raw.endsWith(".gif")
}

private fun DataItemType.toFilterLabel(): String {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> "메모"
        "LINK", "URL" -> "링크"
        "IMAGE", "PHOTO" -> "이미지"
        "FILE", "DOCUMENT" -> "파일"
        "SCREENSHOT", "SCREEN_SHOT" -> "스크린샷"
        else -> name
    }
}

private fun DataItemType.toIcon(): ImageVector {
    return when (name.uppercase()) {
        "TEXT", "MEMO", "NOTE" -> Icons.Default.Note
        "LINK", "URL" -> Icons.Default.InsertLink
        "IMAGE", "PHOTO" -> Icons.Default.Image
        "FILE", "DOCUMENT" -> Icons.Default.Description
        "SCREENSHOT", "SCREEN_SHOT" -> Icons.Default.CameraAlt
        else -> Icons.Default.Description
    }
}

private fun DataItemType.isImageLike(): Boolean {
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

private fun Long.formatDate(): String {
    return SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(this))
}
