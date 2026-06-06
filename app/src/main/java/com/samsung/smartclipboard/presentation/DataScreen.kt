package com.samsung.smartclipboard.presentation

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var permission by remember { mutableStateOf(PermissionStatus.Unknown) }
    var pickerSelected by remember { mutableStateOf(sampleItems.map { it.id }.toSet()) }
    var allowedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

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

    BackHandler(selectMode) { exitSelect() }
    LaunchedEffect(selectMode) { onSelectModeChange(selectMode) }

    val visibleItems = when (permission) {
        PermissionStatus.Unknown, PermissionStatus.Denied, PermissionStatus.Selecting -> emptyList()
        PermissionStatus.Partial -> items.filter { it.id in allowedIds }
        PermissionStatus.Granted -> items
    }
    val filters = listOf("전체", "메모", "링크", "이미지", "파일", "스크린샷")
    val filtered = if (activeFilter == "전체") visibleItems else visibleItems.filter { it.type == activeFilter }

    if (permission == PermissionStatus.Selecting) {
        PhotoPermissionPicker(
            selected = pickerSelected,
            onToggle = { id -> pickerSelected = if (id in pickerSelected) pickerSelected - id else pickerSelected + id },
            onSelectAll = { pickerSelected = sampleItems.map { it.id }.toSet() },
            onClear = { pickerSelected = emptySet() },
            onCancel = { permission = PermissionStatus.Unknown },
            onDone = {
                allowedIds = pickerSelected
                permission = PermissionStatus.Partial
            },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DataHeader(
                selectMode = selectMode,
                selectedCount = selected.size,
                visibleCount = visibleItems.size,
                onDeleteAll = { showDeleteConfirm = true },
                onDeleteSelected = {
                    items = items.filterNot { it.id in selected }
                    exitSelect()
                },
                onToggleSelect = { if (selectMode) exitSelect() else enterSelect() },
            )

            if (showDeleteConfirm) {
                ConfirmBanner(
                    text = "수집한 데이터 ${visibleItems.size}개를 모두 삭제할까요?",
                    onConfirm = {
                        items = emptyList()
                        showDeleteConfirm = false
                    },
                    onCancel = { showDeleteConfirm = false },
                )
            }
            deleteTargetId?.let { id ->
                ConfirmBanner(
                    text = "이 항목을 삭제할까요?",
                    onConfirm = {
                        items = items.filterNot { it.id == id }
                        deleteTargetId = null
                    },
                    onCancel = { deleteTargetId = null },
                )
            }

            if (!selectMode) {
                PermissionCard(
                    permission = permission,
                    allowedCount = allowedIds.size,
                    onGrantAll = { permission = PermissionStatus.Granted },
                    onGrantPartial = { permission = PermissionStatus.Selecting },
                    onDeny = { permission = PermissionStatus.Denied },
                    onReset = { permission = PermissionStatus.Unknown },
                )
            }

            if (selectMode) {
                SelectModeBar(
                    selectedCount = selected.size,
                    onSelectAll = { selected = filtered.map { it.id }.toSet() },
                    onClear = { selected = emptySet() },
                )
            }

            FilterRow(filters, activeFilter, visibleItems.size) { activeFilter = it }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (filtered.isEmpty()) {
                    item { EmptyDataState(permission) }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        DataItemCard(
                            item = item,
                            selected = item.id in selected,
                            selectMode = selectMode,
                            onToggle = {
                                if (selectMode) selected = if (item.id in selected) selected - item.id else selected + item.id
                            },
                            onPreview = { previewItem = item },
                            onDelete = { deleteTargetId = item.id },
                        )
                    }
                }
            }

            if (selectMode && selected.isNotEmpty()) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
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
            PreviewOverlay(item = item, onClose = { previewItem = null })
        }
    }
}

@Composable
fun PhotoPermissionPicker(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        HeaderRow(
            title = "사진 선택",
            subtitle = "허용할 사진을 선택하세요",
            leading = Icons.Default.Close,
            onLeading = onCancel,
            action = {
                GradientButton(text = "완료 ${if (selected.isNotEmpty()) "(${selected.size}개)" else ""}", compact = true, enabled = selected.isNotEmpty(), onClick = onDone)
            },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFBFF))
                .border(1.dp, Color(0xFFF1F5F9))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${selected.size}/${sampleItems.size}개 선택됨", color = AppColors.Slate500, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("모두 선택", color = AppColors.Blue, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onSelectAll))
                Text("선택 해제", color = AppColors.Slate400, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onClear))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(sampleItems) { item ->
                val isSelected = item.id in selected
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) AppColors.Blue else AppColors.Border, RoundedCornerShape(18.dp))
                        .clickable { onToggle(item.id) }
                        .background(Color.White),
                ) {
                    Thumbnail(item, modifier = Modifier.height(104.dp), showCheck = true, checked = isSelected)
                    Column(Modifier.padding(10.dp)) {
                        Text(item.label, color = AppColors.Slate800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(item.date, color = AppColors.Slate400, fontSize = 9.sp)
                    }
                }
            }
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
            Box(Modifier.width(72.dp)) {
                when {
                    !selectMode -> DangerSmallButton("전체 삭제", onClick = onDeleteAll)
                    selectedCount > 0 -> DangerSmallButton("삭제", onClick = onDeleteSelected)
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
            Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                SmallOutlineButton(if (selectMode) "취소" else "선택", active = selectMode, onClick = onToggleSelect)
            }
        }
        Text(
            text = if (selectMode) "${visibleCount}개 중 선택 · 원하는 항목을 고르세요" else "${visibleCount}개 전체 · 텍스트 0 · 링크 0 · 이미지 0 · 스크린샷 ${visibleCount}",
            color = AppColors.Slate400,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
        )
    }
}

@Composable
fun ConfirmBanner(text: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Warning, null, tint = AppColors.Red, modifier = Modifier.size(16.dp))
        Text(text, color = AppColors.Red, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp).weight(1f))
        DangerSmallButton("삭제", onClick = onConfirm)
        Spacer(Modifier.width(6.dp))
        SmallOutlineButton("취소", onClick = onCancel)
    }
}

@Composable
fun PermissionCard(
    permission: PermissionStatus,
    allowedCount: Int,
    onGrantAll: () -> Unit,
    onGrantPartial: () -> Unit,
    onDeny: () -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        when (permission) {
            PermissionStatus.Unknown -> CardBlock {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(Icons.Default.PhotoLibrary, AppColors.Blue, AppColors.BlueSoft, 42)
                    Column(Modifier.weight(1f)) {
                        Text("사진 접근 허용", color = AppColors.Slate800, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("스크린샷을 가져오려면 사진 접근 권한이 필요해요. 데이터는 기기 안에서만 처리됩니다.", color = AppColors.Slate500, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton("모두 허용", modifier = Modifier.weight(1f), onClick = onGrantAll)
                    Button(
                        onClick = onGrantPartial,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.BlueSoft, contentColor = AppColors.Blue),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    ) { Text("일부 허용", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
                TextButton(onClick = onDeny, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("거부", color = AppColors.Slate400, fontSize = 11.sp)
                }
            }
            PermissionStatus.Granted, PermissionStatus.Partial -> CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Default.CameraAlt, AppColors.Blue, AppColors.BlueSoft, 30)
                    Spacer(Modifier.width(8.dp))
                    Text("스크린샷 가져오기", color = AppColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (permission == PermissionStatus.Partial) {
                        Spacer(Modifier.width(6.dp))
                        Pill("일부 허용", Color(0xFFFEF3C7), Color(0xFFD97706))
                    }
                }
                Text(
                    text = if (permission == PermissionStatus.Partial) "${allowedCount}개의 사진에 접근할 수 있어요." else "최근 스크린샷을 다시 스캔하고 이전 항목과 동기화해요.",
                    color = AppColors.Slate500,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton("다시 스캔", icon = Icons.Default.Refresh, modifier = Modifier.weight(1f), onClick = onReset)
                    Button(
                        onClick = onGrantPartial,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.BlueSoft, contentColor = AppColors.Blue),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    ) { Text("권한 변경", fontSize = 11.sp) }
                }
            }
            PermissionStatus.Denied -> Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFAFAFA))
                    .border(1.dp, AppColors.Slate200, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(Icons.Default.Block, AppColors.Slate400, Color(0xFFF1F5F9), 34)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("사진 접근이 거부됨", color = AppColors.Slate500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("스크린샷을 가져올 수 없어요.", color = AppColors.Slate400, fontSize = 10.sp)
                }
                SmallOutlineButton("권한 요청", active = true, onClick = onReset)
            }
            PermissionStatus.Selecting -> Unit
        }
    }
}

@Composable
fun SelectModeBar(selectedCount: Int, onSelectAll: () -> Unit, onClear: () -> Unit) {
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
        Text("선택한 데이터 ${selectedCount}개", color = AppColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("모두 선택", color = AppColors.Blue, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onSelectAll))
            Row(Modifier.clickable(onClick = onClear), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Replay, null, tint = AppColors.Slate500, modifier = Modifier.size(12.dp))
                Text("초기화", color = AppColors.Slate500, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun FilterRow(filters: List<String>, active: String, screenshotCount: Int, onSelect: (String) -> Unit) {
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
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (filter == "스크린샷") "$filter $screenshotCount" else filter,
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
            .clickable(enabled = selectMode, onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AppColors.Blue else AppColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            Thumbnail(item = item, modifier = Modifier.height(88.dp), showCheck = selectMode, checked = selected)
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
                Text(item.preview, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(item.type, Color.White, AppColors.Blue)
                    Spacer(Modifier.width(6.dp))
                    Text(item.mime, color = AppColors.Slate400, fontSize = 9.sp)
                }
                Text(item.name, color = AppColors.Slate800, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.date, color = AppColors.Slate400, fontSize = 9.sp)
            }
            if (!selectMode) {
                DangerSmallButton("삭제", icon = Icons.Default.Delete, onClick = onDelete)
            }
        }
    }
}

@Composable
fun EmptyDataState(permission: PermissionStatus) {
    val message = when (permission) {
        PermissionStatus.Unknown -> "권한을 허용하면 스크린샷 목록이 표시됩니다."
        PermissionStatus.Denied -> "사진 접근이 거부되었어요."
        else -> "수집한 데이터가 없어요."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBubble(if (permission == PermissionStatus.Unknown) Icons.Default.Shield else Icons.Default.CameraAlt, AppColors.Slate400, Color(0xFFF1F5F9), 58)
        Spacer(Modifier.height(12.dp))
        Text(message, color = AppColors.Slate400, fontSize = 13.sp)
        Text("새 항목을 추가해 보세요.", color = Color(0xFFCBD5E1), fontSize = 11.sp)
    }
}

@Composable
fun PreviewOverlay(item: ClipboardItem, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Slate900)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(item.date, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            IconButtonPlain(Icons.Default.Close, Color.White, onClick = onClose)
        }
        Box(Modifier.fillMaxSize().padding(top = 18.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(item.color.copy(alpha = 0.25f), item.color.copy(alpha = 0.50f)))),
            ) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SkeletonLine(item.color, Modifier.width(100.dp).height(14.dp))
                        SkeletonLine(item.color, Modifier.width(64.dp).height(14.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(5) { SkeletonLine(item.color, Modifier.fillMaxWidth(if (it % 2 == 0) 1f else 0.75f).height(10.dp)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SkeletonLine(item.color, Modifier.width(112.dp).height(38.dp))
                        SkeletonLine(item.color, Modifier.width(80.dp).height(38.dp))
                    }
                }
                IconBubble(Icons.Default.CameraAlt, item.color, item.color.copy(alpha = 0.18f), 68, modifier = Modifier.align(Alignment.Center))
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(AppColors.Slate900.copy(alpha = 0.78f))
                        .padding(16.dp),
                ) {
                    Text(item.preview, color = Color.White, fontSize = 11.sp)
                    Text(item.name, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
