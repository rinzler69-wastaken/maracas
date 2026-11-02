package id.josjis.maracas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.josjis.maracas.R
import id.josjis.maracas.ui.theme.MonetTones
import id.josjis.maracas.ui.theme.monetTones

@Composable
fun ProtectionSection(
    masterOn: Boolean,
    reachFlipEnabled: Boolean,
    pocketEnabled: Boolean,
    onToggleReachFlip: (Boolean) -> Unit,
    onTogglePocket: (Boolean) -> Unit,
    onShowReachFlipInfo: () -> Unit,
    onShowPocketInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Top Card: Reach / Flip (top rounded 24dp, bottom flat 0dp)
        ProtectionRowCard(
            title = stringResource(R.string.reach_flip_title),
            subtitle = stringResource(R.string.reach_flip_subtitle),
            iconRes = R.drawable.ic_mobile_hand,
            isChecked = reachFlipEnabled,
            masterOn = masterOn,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            onCheckedChange = onToggleReachFlip,
            onInfoClick = onShowReachFlipInfo
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Bottom Card: Pocket Protection (top flat 0dp, bottom rounded 24dp)
        ProtectionRowCard(
            title = stringResource(R.string.pocket_protection_title),
            subtitle = stringResource(R.string.pocket_protection_subtitle),
            iconRes = R.drawable.ic_shield,
            isChecked = pocketEnabled,
            masterOn = masterOn,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            onCheckedChange = onTogglePocket,
            onInfoClick = onShowPocketInfo
        )
    }
}

@Composable
private fun ProtectionRowCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    isChecked: Boolean,
    masterOn: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = masterOn && isChecked
    val tones = monetTones

    val rowBg by animateColorAsState(
        targetValue = if (active) tones.protectionRowBackground else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "protectionRowBg"
    )

    val rowFg = when {
        active -> tones.protectionRowForeground
        masterOn -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = MonetTones.DISABLED_ALPHA)
    }

    val subtitleFg = when {
        active -> tones.mutedSubtitle(rowFg, rowBg)
        masterOn -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MonetTones.DISABLED_ALPHA)
    }

    val iconBg = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh
    val iconFg = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = rowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 76.dp)
                .padding(start = 16.dp, top = 18.dp, end = 12.dp, bottom = 18.dp)
        ) {
            // Icon in rounded container (38x38dp, 12dp radius)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .alpha(if (masterOn) 1f else MonetTones.DISABLED_ALPHA)
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconFg,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Labels: non-pressable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = rowFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleFg,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Info button
            IconButton(
                onClick = onInfoClick,
                enabled = masterOn,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.info_cd, title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (masterOn) 1f else MonetTones.DISABLED_ALPHA
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Switch with custom track & thumb colors
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = masterOn,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = MonetTones.DISABLED_ALPHA),
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = MonetTones.DISABLED_ALPHA)
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
