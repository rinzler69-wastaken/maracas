package id.josjis.maracas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import id.josjis.maracas.R

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.got_it),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun AppearanceDialog(
    initialMaterialYou: Boolean,
    initialTheme: String,
    onApply: (materialYou: Boolean, theme: String) -> Unit,
    onDismiss: () -> Unit
) {
    var materialYou by remember { mutableStateOf(initialMaterialYou) }
    var selectedTheme by remember { mutableStateOf(initialTheme) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.appearance_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Material You row
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.material_you_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.material_you_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = materialYou,
                            onCheckedChange = { materialYou = it }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.theme_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                )

                // Theme Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeChip(
                        title = stringResource(R.string.theme_system),
                        iconRes = R.drawable.ic_theme_auto,
                        isSelected = selectedTheme == "system",
                        onClick = { selectedTheme = "system" },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeChip(
                        title = stringResource(R.string.theme_light),
                        iconRes = R.drawable.ic_theme_light,
                        isSelected = selectedTheme == "light",
                        onClick = { selectedTheme = "light" },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeChip(
                        title = stringResource(R.string.theme_dark),
                        iconRes = R.drawable.ic_theme_dark,
                        isSelected = selectedTheme == "dark",
                        onClick = { selectedTheme = "dark" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Done button
                TextButton(
                    onClick = {
                        onApply(materialYou, selectedTheme)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.colorPrimaryContainerOrFallback()
    else MaterialTheme.colorScheme.surfaceContainer
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun androidx.compose.material3.ColorScheme.colorPrimaryContainerOrFallback() = this.primaryContainer

@Composable
fun ShakeActionsDialog(
    currentAction: String,
    onSelectAction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.shake_action_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ActionItemCard(
                    title = stringResource(R.string.action_default),
                    iconRes = R.drawable.ic_power,
                    isSelected = currentAction == "default",
                    onClick = {
                        onSelectAction("default")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ActionItemCard(
                    title = stringResource(R.string.action_flashlight),
                    iconRes = R.drawable.ic_flashlight,
                    isSelected = currentAction == "flashlight",
                    onClick = {
                        onSelectAction("flashlight")
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionItemCard(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = fg,
                modifier = Modifier
                    .size(22.dp)
                    .padding(end = 12.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = fg,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
