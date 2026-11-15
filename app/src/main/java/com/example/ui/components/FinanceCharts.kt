package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.formatMoney
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OrangePrimary
import com.example.util.CategoryBreakdown
import com.example.util.CategoryIconMapper
import com.example.util.DailyPoint
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun CategoryBadgeIcon(
    iconKey: String,
    colorHex: Long,
    size: Dp = 44.dp,
    contentDescription: String? = null
) {
    // Solid rounded square badge with crisp white icon matching CoinPie screenshots
    val badgeColor = Color(colorHex)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIconMapper.iconFor(iconKey),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

@Composable
fun DemoBadgeChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_wallet),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 160.dp, height = 120.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(
    points: List<DailyPoint>,
    currencyCode: String,
    hideBalances: Boolean,
    incomeLabel: String,
    expenseLabel: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 155.dp
) {
    if (points.isEmpty()) return
    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val maxVal = max(1.0, points.maxOf { max(it.income, it.expense) })
    val barAnim = remember { Animatable(0f) }
    LaunchedEffect(points) {
        barAnim.snapTo(0f)
        barAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing)
        )
    }
    val animProgress = barAnim.value
    val animatedSelectedIdx by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessLow),
        label = "bar_slot_slide"
    )
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Column(modifier = modifier.fillMaxWidth()) {
        val selected = points.getOrNull(selectedIndex)
        if (selected != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$incomeLabel: ${formatMoney(selected.income, currencyCode, hideBalances)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$expenseLabel: ${formatMoney(selected.expense, currencyCode, hideBalances)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val slotWidth = size.width.toFloat() / points.size.coerceAtLeast(1)
                        val idx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                        selectedIndex = idx
                    }
                }
        ) {
            val slotWidth = size.width / points.size.coerceAtLeast(1)
            val chartHeight = size.height - 16f

            for (i in 0..2) {
                val y = chartHeight * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }

            drawRoundRect(
                color = highlightBg,
                topLeft = Offset(animatedSelectedIdx * slotWidth + 2f, 0f),
                size = Size(slotWidth - 4f, chartHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            points.forEachIndexed { index, pt ->
                val slotLeft = index * slotWidth
                val barW = (slotWidth * 0.28f).coerceIn(8f, 26f)
                val gap = 4f
                val centerX = slotLeft + slotWidth / 2f

                val incH = ((pt.income / maxVal).toFloat() * (chartHeight - 8f) * animProgress)
                    .coerceAtLeast(if (pt.income > 0) 6f else 0f)
                val expH = ((pt.expense / maxVal).toFloat() * (chartHeight - 8f) * animProgress)
                    .coerceAtLeast(if (pt.expense > 0) 6f else 0f)

                if (incH > 0f) {
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(centerX - barW - gap / 2f, chartHeight - incH),
                        size = Size(barW, incH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
                if (expH > 0f) {
                    drawRoundRect(
                        color = OrangePrimary,
                        topLeft = Offset(centerX + gap / 2f, chartHeight - expH),
                        size = Size(barW, expH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { index, pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { selectedIndex = index }
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdown>,
    currencyCode: String,
    hideBalances: Boolean,
    language: String,
    centerSubtitle: String,
    onCategoryClick: ((CategoryBreakdown) -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (items.isEmpty()) return
    // -1 means show total in center (CoinPie default state); tapping a slice focuses that slice
    var selectedIdx by remember(items) { mutableIntStateOf(-1) }
    val total = remember(items) { items.sumOf { it.amount }.coerceAtLeast(1.0) }
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(items) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 740, easing = FastOutSlowInEasing)
        )
    }
    val sweepProgress = sweepAnim.value
    val activeExtraWidth by animateFloatAsState(
        targetValue = if (selectedIdx >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "donut_slice_focus"
    )
    val trackBg = MaterialTheme.colorScheme.surfaceVariant
    val boxSize = if (compact) 144.dp else 214.dp
    val canvasSize = if (compact) 136.dp else 204.dp
    val ringStrokeDp = if (compact) 20.dp else 34.dp
    val extraFocusDp = if (compact) 4.dp else 7.dp

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(boxSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(canvasSize)
                    .pointerInput(items) {
                        detectTapGestures { tap ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tap.x - cx
                            val dy = tap.y - cy
                            if (hypot(dx, dy) <= size.width * 0.22f) {
                                selectedIdx = -1
                            } else {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f
                                var acc = 0f
                                items.forEachIndexed { idx, item ->
                                    val sweep = (item.amount / total).toFloat() * 360f
                                    if (angle in acc..(acc + sweep)) {
                                        selectedIdx = if (selectedIdx == idx) -1 else idx
                                        return@detectTapGestures
                                    }
                                    acc += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = ringStrokeDp.toPx()
                drawArc(
                    color = trackBg,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                var startAngle = -90f
                items.forEachIndexed { idx, item ->
                    val sweep = (((item.amount / total).toFloat() * 360f) * sweepProgress).coerceAtLeast(2.0f)
                    val isSel = idx == selectedIdx
                    drawArc(
                        color = Color(item.colorHex),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2.0f).coerceAtLeast(1.2f),
                        useCenter = false,
                        style = Stroke(
                            width = if (isSel) strokeWidth + (extraFocusDp.toPx() * activeExtraWidth) else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                    startAngle += sweep
                }
            }

            val selected = items.getOrNull(selectedIdx)
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(300, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300, easing = FastOutSlowInEasing)))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "donut_center_anim"
            ) { currentSel ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = if (compact) 14.dp else 18.dp)
                ) {
                    Text(
                        text = currentSel?.displayName(language) ?: centerSubtitle,
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(if (compact) 1.dp else 2.dp))
                    Text(
                        text = formatMoney(currentSel?.amount ?: total, currencyCode, hideBalances),
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
        ) {
            items.forEachIndexed { idx, item ->
                key(item.categoryId) {
                val isSelected = idx == selectedIdx
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(item.colorHex).copy(alpha = 0.09f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) Color(item.colorHex).copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedIdx = if (selectedIdx == idx) -1 else idx
                            onCategoryClick?.invoke(item)
                        }
                        .padding(horizontal = 6.dp, vertical = if (compact) 4.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadgeIcon(
                        iconKey = item.iconKey,
                        colorHex = item.colorHex,
                        size = if (compact) 34.dp else 42.dp,
                        contentDescription = item.displayName(language)
                    )
                    Spacer(Modifier.width(if (compact) 10.dp else 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.displayName(language),
                            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.percentage.toInt()}%",
                            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatMoney(item.amount, currencyCode, hideBalances),
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                    )
                }
                }
            }
        }
    }
}
