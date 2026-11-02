package id.josjis.maracas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.josjis.maracas.R
import id.josjis.maracas.ui.SensMode
import id.josjis.maracas.ui.theme.MonetTones
import id.josjis.maracas.ui.theme.monetTones

@Composable
fun WakeMethodSelector(
    masterOn: Boolean,
    shakeEnabled: Boolean,
    liftEnabled: Boolean,
    expandedSensMode: SensMode?,
    onToggleShake: () -> Unit,
    onToggleLift: () -> Unit,
    onAdjustShake: () -> Unit,
    onAdjustLift: () -> Unit,
    onShowShakeInfo: () -> Unit,
    onShowLiftInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        WakeCard(
            title = stringResource(R.string.shake_title),
            subtitle = stringResource(R.string.shake_subtitle),
            iconRes = R.drawable.ic_vibration,
            masterOn = masterOn,
            selected = shakeEnabled,
            adjustActive = expandedSensMode == SensMode.SHAKE,
            onCardClick = onToggleShake,
            onAdjustClick = onAdjustShake,
            onInfoClick = onShowShakeInfo,
            adjustContentDescription = stringResource(R.string.adjust_cd, stringResource(R.string.shake_title)),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        WakeCard(
            title = stringResource(R.string.lift_title),
            subtitle = stringResource(R.string.lift_subtitle),
            iconRes = R.drawable.ic_mobile_theft,
            masterOn = masterOn,
            selected = liftEnabled,
            adjustActive = expandedSensMode == SensMode.LIFT,
            onCardClick = onToggleLift,
            onAdjustClick = onAdjustLift,
            onInfoClick = onShowLiftInfo,
            adjustContentDescription = stringResource(R.string.adjust_cd, stringResource(R.string.lift_title)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WakeCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    masterOn: Boolean,
    selected: Boolean,
    adjustActive: Boolean,
    onCardClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onInfoClick: () -> Unit,
    adjustContentDescription: String,
    modifier: Modifier = Modifier
) {
    val active = masterOn && selected
    val tones = monetTones

    val cardBg by animateColorAsState(
        targetValue = if (active) tones.wakeEnabledBackground else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "cardBg"
    )

    val fg = when {
        active -> tones.wakeEnabledForeground
        masterOn -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = MonetTones.DISABLED_ALPHA)
    }

    val subtitleFg = when {
        active -> tones.mutedSubtitle(fg, cardBg)
        masterOn -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MonetTones.DISABLED_ALPHA)
    }

    val iconBg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val iconFg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(106.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = masterOn, onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)
        ) {
            // Top row: Main icon + Checkmark (if active) + Info button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg)
                        .alpha(if (masterOn) 1f else MonetTones.DISABLED_ALPHA)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconFg,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (active) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp)
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    enabled = masterOn,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = stringResource(R.string.info_cd, title),
                        tint = fg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom row: Title / Subtitle + Adjust Button
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = fg
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleFg,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Adjust buttonlet
                val usable = masterOn && selected
                val adjustBg = when {
                    adjustActive -> MaterialTheme.colorScheme.tertiary
                    usable -> tones.wakeEnabledBackground
                    else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f)
                }
                val adjustFg = when {
                    adjustActive -> MaterialTheme.colorScheme.onTertiary
                    usable -> tones.wakeEnabledForeground
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MonetTones.DISABLED_ALPHA)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(adjustBg)
                        .clickable(enabled = usable, onClick = onAdjustClick)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tune),
                        contentDescription = adjustContentDescription,
                        tint = adjustFg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
