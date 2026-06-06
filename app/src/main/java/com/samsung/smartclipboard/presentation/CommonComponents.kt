package com.samsung.smartclipboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimpleTopHeader(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, AppColors.Border)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = AppColors.Slate800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = AppColors.Slate400, fontSize = 10.sp)
    }
}

@Composable
fun HeaderRow(
    title: String,
    subtitle: String,
    leading: ImageVector,
    onLeading: () -> Unit,
    modifier: Modifier = Modifier,
    leadingTint: Color = AppColors.Slate500,
    badgeIcon: ImageVector? = null,
    badgeColor: Color = AppColors.Blue,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, AppColors.Border)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonPlain(leading, leadingTint, bg = Color(0xFFF1F5F9), onClick = onLeading)
        Spacer(Modifier.width(12.dp))
        badgeIcon?.let {
            IconBubble(it, badgeColor, badgeColor.copy(alpha = 0.10f), 30)
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = AppColors.Slate800, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = AppColors.Slate400, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}

@Composable
fun CardBlock(
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    borderColor: Color = AppColors.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    compact: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 10.dp else 16.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (enabled) BlueGradient else SolidColor(AppColors.Slate200))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 7.dp else 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            icon?.let {
                Icon(it, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = Color.White, fontSize = if (compact) 11.sp else 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DangerSmallButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, tint = AppColors.Red, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = AppColors.Red, fontSize = 10.sp)
    }
}

@Composable
fun SmallOutlineButton(text: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) AppColors.Blue else Color.White)
            .border(1.dp, if (active) AppColors.Blue else AppColors.Slate200, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, color = if (active) Color.White else AppColors.Slate500, fontSize = 10.sp)
    }
}

@Composable
fun Pill(text: String, bg: Color, color: Color) {
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun StatChip(icon: ImageVector, label: String, color: Color) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (color == AppColors.Blue) Color.White else AppColors.Surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 10.sp)
    }
}

@Composable
fun IconBubble(icon: ImageVector, tint: Color, bg: Color, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.45f).dp))
    }
}

@Composable
fun IconButtonPlain(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    bg: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
fun Thumbnail(item: ClipboardItem, modifier: Modifier = Modifier, showCheck: Boolean = false, checked: Boolean = false) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(item.color.copy(alpha = 0.10f), item.color.copy(alpha = 0.24f)))),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonLine(item.color, Modifier.width(64.dp).height(8.dp))
                SkeletonLine(item.color, Modifier.width(42.dp).height(8.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SkeletonLine(item.color, Modifier.fillMaxWidth().height(6.dp))
                SkeletonLine(item.color, Modifier.fillMaxWidth(0.76f).height(6.dp))
                SkeletonLine(item.color, Modifier.fillMaxWidth(0.84f).height(6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonLine(item.color, Modifier.width(64.dp).height(20.dp))
                SkeletonLine(item.color, Modifier.width(48.dp).height(20.dp))
            }
        }
        Icon(Icons.Default.CameraAlt, null, tint = item.color.copy(alpha = 0.40f), modifier = Modifier.align(Alignment.Center).size(26.dp))
        if (showCheck) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (checked) AppColors.Blue else Color.White.copy(alpha = 0.90f))
                    .border(2.dp, if (checked) AppColors.Blue else Color(0xFFCBD5E1), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun SkeletonLine(color: Color, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(5.dp)).background(color.copy(alpha = 0.36f)))
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(AppColors.Slate200))
}

@Composable
fun FlowStepRow(steps: List<Pair<String, Boolean>>) {
    Row(
        Modifier
            .padding(vertical = 10.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            Pill(step.first, if (step.second) AppColors.Blue else Color.White, if (step.second) Color.White else AppColors.Slate400)
            if (index < steps.lastIndex) {
                Text("→", color = Color(0xFFCBD5E1), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
fun FieldBlock(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, color = AppColors.Slate400, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        content()
    }
}

@Composable
fun ReadOnlyBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .padding(12.dp),
    ) {
        content()
    }
}

