package id.josjis.maracas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.josjis.maracas.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeaderCard(
    masterOn: Boolean,
    materialYouOn: Boolean,
    themeMode: String,
    isDynamicColorAvailable: Boolean,
    onToggleMaster: () -> Unit,
    onToggleMaterialYou: () -> Unit,
    onCycleThemeQuick: () -> Unit,
    onSetThemeSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerBg by animateColorAsState(
        targetValue = if (masterOn) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(250),
        label = "headerBg"
    )

    val headerFg by animateColorAsState(
        targetValue = if (masterOn) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(250),
        label = "headerFg"
    )

    val cardModifier = if (masterOn) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .dottedBorder(
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                cornerRadius = 28.dp
            )
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = headerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = cardModifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = headerFg,
                modifier = Modifier.weight(1f)
            )

            // Circular Master Power Switch
            HeaderCircleButton(
                iconRes = R.drawable.ic_power,
                contentDescription = stringResource(R.string.master_cd),
                activeBg = MaterialTheme.colorScheme.primary,
                activeFg = MaterialTheme.colorScheme.onPrimary,
                isActive = masterOn,
                onClick = onToggleMaster
            )

            // Material You toggle button (direct toggle, no dialog)
            if (isDynamicColorAvailable) {
                Spacer(modifier = Modifier.width(6.dp))
                HeaderCircleButton(
                    iconRes = R.drawable.ic_material_you,
                    contentDescription = stringResource(R.string.material_you_cd),
                    activeBg = MaterialTheme.colorScheme.secondary,
                    activeFg = MaterialTheme.colorScheme.onSecondary,
                    isActive = masterOn && materialYouOn,
                    onClick = onToggleMaterialYou
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Theme toggle: tap = light/dark, long-click = system
            val themeIcon = when (themeMode) {
                "light" -> R.drawable.ic_theme_light
                "dark" -> R.drawable.ic_theme_dark
                else -> R.drawable.ic_theme_contrast
            }

            HeaderCircleButton(
                iconRes = themeIcon,
                contentDescription = stringResource(R.string.theme_cd),
                activeBg = MaterialTheme.colorScheme.tertiary,
                activeFg = MaterialTheme.colorScheme.onTertiary,
                isActive = masterOn,
                onClick = onCycleThemeQuick,
                onLongClick = onSetThemeSystem
            )
        }
    }
}

@Composable
private fun HeaderCircleButton(
    iconRes: Int,
    contentDescription: String,
    activeBg: Color,
    activeFg: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val bg = if (isActive) activeBg else Color.Transparent
    val fg = if (isActive) activeFg else MaterialTheme.colorScheme.onSurfaceVariant
    val borderModifier = if (isActive) Modifier else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(bg)
            .bounceCombinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(24.dp)
        )
    }
}
