package id.josjis.maracas.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

fun Modifier.bounceClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(0.84f, spring(stiffness = Spring.StiffnessMediumLow))
        } else {
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
        }
    }

    this
        .scale(scale.value)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.bounceCombinedClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(0.84f, spring(stiffness = Spring.StiffnessMediumLow))
        } else {
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
        }
    }

    this
        .scale(scale.value)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
}
