package id.josjis.maracas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.josjis.maracas.R
import id.josjis.maracas.ui.ProximityState
import id.josjis.maracas.ui.theme.LocalDynamicColor
import id.josjis.maracas.ui.theme.MaracasYellow
import id.josjis.maracas.ui.theme.proximityDotPrimary
import id.josjis.maracas.ui.theme.tertiaryContainerProximity

@Composable
fun ProximitySensorCard(
    masterOn: Boolean,
    proximityState: ProximityState,
    modifier: Modifier = Modifier
) {
    val cardBg by animateColorAsState(
        targetValue = if (masterOn) MaterialTheme.colorScheme.tertiaryContainerProximity else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "proxCardBg"
    )

    // Un-muted text and status whether master is ON or OFF
    val fg = if (masterOn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 76.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = stringResource(R.string.prox_title),
                style = MaterialTheme.typography.titleMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when (proximityState) {
                    ProximityState.NEAR -> stringResource(R.string.status_on)
                    ProximityState.FAR -> stringResource(R.string.status_off)
                    ProximityState.UNAVAILABLE -> stringResource(R.string.status_unavailable)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    modifier = Modifier.padding(end = 10.dp)
                )

                ProximityDotIndicator(
                    state = proximityState,
                    primaryColor = MaterialTheme.colorScheme.proximityDotPrimary,
                    hasBackingCircle = LocalDynamicColor.current
                )
            }
        }
    }
}

@Composable
private fun ProximityDotIndicator(
    state: ProximityState,
    primaryColor: Color,
    hasBackingCircle: Boolean = true
) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        if (!hasBackingCircle) {
            // Simplified fallback path (no backing circle)
            when (state) {
                ProximityState.UNAVAILABLE -> {
                    // Unavailable: neutral gray ring / dot
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.6f),
                        radius = radius,
                        center = center
                    )
                }

                ProximityState.NEAR -> {
                    // Clean solid yellow dot with no backing ring or outline
                    drawCircle(
                        color = MaracasYellow,
                        radius = radius,
                        center = center
                    )
                }

                ProximityState.FAR -> {
                    // Hollow yellow ring with no backing fill (card flat gray shines through)
                    val strokeWidth = 2.5.dp.toPx()
                    drawCircle(
                        color = MaracasYellow,
                        radius = radius - strokeWidth / 2f,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        } else {
            // Dynamic color Material You path (preserves backing circle and outlines)
            when (state) {
                ProximityState.UNAVAILABLE -> {
                    // Unavailable: neutral gray ring / dot
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.6f),
                        radius = radius,
                        center = center
                    )
                }

                ProximityState.NEAR -> {
                    // Solid yellow dot with primary outline
                    val strokeWidth = 2.dp.toPx()
                    drawCircle(
                        color = MaracasYellow,
                        radius = radius - strokeWidth / 2f,
                        center = center
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = radius - strokeWidth / 2f,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                }

                ProximityState.FAR -> {
                    // Primary outer background, 2dp inset yellow ring, 5dp inset primary inner fill
                    val stroke2dp = 2.dp.toPx()
                    val stroke5dp = 5.dp.toPx()

                    // Outer primary fill
                    drawCircle(
                        color = primaryColor,
                        radius = radius,
                        center = center
                    )
                    // Yellow middle fill
                    drawCircle(
                        color = MaracasYellow,
                        radius = (radius - stroke2dp).coerceAtLeast(0f),
                        center = center
                    )
                    // Inner primary fill
                    drawCircle(
                        color = primaryColor,
                        radius = (radius - stroke5dp).coerceAtLeast(0f),
                        center = center
                    )
                }
            }
        }
    }
}
