package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.i18n.formatMoney
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun AnimatedEntry(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (index > 0) {
            delay(index.coerceAtMost(5) * 65L)
        }
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { 28 }
            )
    ) {
        content()
    }
}

@Composable
fun rememberPressBounce(pressedScale: Float = 0.96f): Pair<MutableInteractionSource, Modifier> {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_bounce_scale"
    )
    return interactionSource to Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun AnimatedMoneyText(
    amount: Double,
    currencyCode: String,
    hideBalances: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    prefix: String = ""
) {
    var startValue by remember { mutableDoubleStateOf(amount) }
    var endValue by remember { mutableDoubleStateOf(amount) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(amount) {
        if (abs(amount - endValue) > 0.001) {
            startValue = startValue + (endValue - startValue) * progress.value
            endValue = amount
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
        }
    }

    val displayed = if (progress.value >= 0.999f) {
        amount
    } else {
        startValue + (endValue - startValue) * progress.value
    }

    Text(
        text = "$prefix${formatMoney(displayed, currencyCode, hideBalances)}",
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}
