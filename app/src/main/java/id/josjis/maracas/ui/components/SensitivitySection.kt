package id.josjis.maracas.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.josjis.maracas.R
import id.josjis.maracas.ui.SensMode
import id.josjis.maracas.ui.theme.MonetTones
import id.josjis.maracas.ui.theme.monetTones
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SensitivitySection(
    masterOn: Boolean,
    shakeEnabled: Boolean,
    liftEnabled: Boolean,
    expandedSensMode: SensMode?,
    shakeSens: Int,
    shakeAction: String,
    liftSens: String,
    onSensChanged: (Int) -> Unit,
    onSetShakeAction: (String) -> Unit,
    onSetLiftSens: (String) -> Unit,
    onShowShakeSensInfo: () -> Unit,
    onShowLiftSensInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(154.dp)
    ) {
        AnimatedContent(
            targetState = expandedSensMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "sensitivitySection"
        ) { mode ->
            when (mode) {
                null -> SensitivityPlaceholderCard(masterOn = masterOn)
                SensMode.SHAKE -> ShakeSensitivityPanel(
                    masterOn = masterOn,
                    shakeEnabled = shakeEnabled,
                    sensValue = shakeSens,
                    shakeAction = shakeAction,
                    onSensChanged = onSensChanged,
                    onSetShakeAction = onSetShakeAction,
                    onShowInfo = onShowShakeSensInfo
                )
                SensMode.LIFT -> LiftSensitivityPanel(
                    masterOn = masterOn,
                    liftEnabled = liftEnabled,
                    liftSens = liftSens,
                    onSetLiftSens = onSetLiftSens,
                    onShowInfo = onShowLiftSensInfo
                )
            }
        }
    }
}

@Composable
private fun SensitivityPlaceholderCard(
    masterOn: Boolean,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (masterOn) {
        modifier
            .fillMaxSize()
            .dottedBorder(
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                cornerRadius = 24.dp
            )
    } else {
        modifier.fillMaxSize()
    }

    val inlineContentMap = remember {
        mapOf(
            "adjust_icon" to InlineTextContent(
                Placeholder(
                    width = 18.sp,
                    height = 18.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    val placeholderRaw = stringResource(R.string.sens_placeholder_text)
    val annotatedString = remember(placeholderRaw) {
        val textParts = placeholderRaw.split("{adjust}")
        buildAnnotatedString {
            if (textParts.isNotEmpty()) {
                append(textParts[0])
                appendInlineContent("adjust_icon", "[adjust]")
                if (textParts.size > 1) {
                    append(textParts[1])
                }
            } else {
                append(placeholderRaw)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = cardModifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = annotatedString,
                inlineContent = inlineContentMap,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (masterOn) 1f else MonetTones.DISABLED_ALPHA)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShakeSensitivityPanel(
    masterOn: Boolean,
    shakeEnabled: Boolean,
    sensValue: Int,
    shakeAction: String,
    onSensChanged: (Int) -> Unit,
    onSetShakeAction: (String) -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usable = masterOn && shakeEnabled
    val tones = monetTones
    val isFlashlight = shakeAction == "flashlight"
    val maxSens = if (isFlashlight) 45f else 100f

    // Local slider state prevents IPC and disk churn on every drag frame
    var sliderPos by remember(sensValue, maxSens) {
        mutableFloatStateOf(sensValue.toFloat().coerceIn(0f, maxSens))
    }

    val effectiveV = if (isFlashlight) sliderPos.coerceAtMost(45f) else sliderPos
    var g = 4.0f - 2.2f * (effectiveV / 100f)
    if (isFlashlight) {
        g = g.coerceAtLeast(3.0f)
    }

    // Safely format the string without throwing IllegalFormatConversionException
    val formatPattern = stringResource(R.string.sens_title_value)
    val titleText = remember(formatPattern, g) {
        try {
            String.format(Locale.US, formatPattern, g)
        } catch (_: Exception) {
            "Shake Sensitivity • about ${String.format(Locale.US, "%.1f", g)} g"
        }
    }

    val titleFg = if (masterOn) tones.wakeEnabledForeground else MaterialTheme.colorScheme.onSurface
    val infoFg = if (masterOn) tones.wakeEnabledForeground else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = tones.sensitivityPanelBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp)
        ) {
            // Header: Title + Info button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleFg,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onShowInfo,
                    enabled = usable,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = stringResource(R.string.info_cd, stringResource(R.string.sens_title)),
                        tint = infoFg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body: Slider Column + Action button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = sliderPos,
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = { onSensChanged(sliderPos.roundToInt()) },
                        valueRange = 0f..maxSens,
                        enabled = usable,
                        colors = SliderDefaults.colors(
                            thumbColor = if (usable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            activeTrackColor = if (usable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sens_end_low),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.sens_end_high),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Actions Button Column: One-tap toggle between Wake (default) and Flashlight
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val actionIconRes = if (isFlashlight) R.drawable.ic_flashlight else R.drawable.ic_power
                    val actionBg = if (usable) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainer
                    val actionFg = if (usable) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    val actionLabel = if (isFlashlight) stringResource(R.string.action_flashlight) else stringResource(R.string.action_default)

                    val buttonText = stringResource(R.string.action_button_label)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(actionBg)
                            .bounceClickable(
                                enabled = usable,
                                onClick = {
                                    val nextAction = if (isFlashlight) "default" else "flashlight"
                                    onSetShakeAction(nextAction)
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(actionIconRes),
                            contentDescription = "$buttonText: $actionLabel",
                            tint = actionFg,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiftSensitivityPanel(
    masterOn: Boolean,
    liftEnabled: Boolean,
    liftSens: String,
    onSetLiftSens: (String) -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usable = masterOn && liftEnabled
    val tones = monetTones
    val isGentle = liftSens != "firm"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = tones.sensitivityPanelBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        ) {
            // Header: Title + Info button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.lift_sens_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (masterOn) tones.wakeEnabledForeground else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onShowInfo,
                    enabled = usable,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = stringResource(R.string.info_cd, stringResource(R.string.lift_sens_title)),
                        tint = if (masterOn) tones.wakeEnabledForeground else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Buttonlets Row (Gentle vs Firm)
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                LiftButtonlet(
                    title = stringResource(R.string.lift_gentle),
                    selected = isGentle,
                    masterOn = masterOn,
                    usable = usable,
                    onClick = { onSetLiftSens("gentle") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                LiftButtonlet(
                    title = stringResource(R.string.lift_firm),
                    selected = !isGentle,
                    masterOn = masterOn,
                    usable = usable,
                    onClick = { onSetLiftSens("firm") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LiftButtonlet(
    title: String,
    selected: Boolean,
    masterOn: Boolean,
    usable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = masterOn && selected
    val tones = monetTones
    val panelBg = if (masterOn) tones.sensitivityPanelBackground else MaterialTheme.colorScheme.surfaceContainer
    val bg = if (active) MaterialTheme.colorScheme.tertiary else panelBg
    val fg = when {
        active -> MaterialTheme.colorScheme.onTertiary
        masterOn -> tones.wakeEnabledForeground
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = MonetTones.DISABLED_ALPHA)
    }

    val borderModifier = if (!active && masterOn) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            shape = CircleShape
        )
    } else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(bg)
            .bounceClickable(enabled = usable, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (active) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 6.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = fg
            )
        }
    }
}
