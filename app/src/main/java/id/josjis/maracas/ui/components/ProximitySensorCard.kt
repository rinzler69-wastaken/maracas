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
import id.josjis.maracas.ui.theme.MaracasYellow
import id.josjis.maracas.ui.theme.monetTones

@Composable
fun ProximitySensorCard(
    masterOn: Boolean,
    proximityState: ProximityState,
    modifier: Modifier = Modifier
) {
    val tones = monetTones

    val cardBg by animateColorAsState(
        targetValue = if (masterOn) tones.proximityCardBackground else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "proxCardBg"
    )

    val fg = if (masterOn) tones.proximityCardForeground else MaterialTheme.colorScheme.onSurface

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
                    masterOn = masterOn,
                    primaryColor = tones.proximityDotPrimary
                )
            }
        }
    }
}

@Composable
private fun ProximityDotIndicator(
    state: ProximityState,
    masterOn: Boolean,
    primaryColor: Color
) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

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
                if (masterOn) {
                    // NEAR when Master ON: primary outline around solid yellow dot
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
                } else {
                    // NEAR when Master OFF: solid yellow dot
                    drawCircle(
                        color = MaracasYellow,
                        radius = radius,
                        center = center
                    )
                }
            }

            ProximityState.FAR -> {
                if (masterOn) {
                    // FAR when Master ON: primary outer background, 2dp inset yellow ring, 5dp inset primary inner fill
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
                } else {
                    // FAR when Master OFF: yellow ring
                    val strokeWidth = 2.dp.toPx()
                    drawCircle(
                        color = MaracasYellow,
                        radius = radius - strokeWidth / 2f,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}
