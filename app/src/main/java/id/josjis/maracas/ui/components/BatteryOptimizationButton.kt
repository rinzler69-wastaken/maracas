package id.josjis.maracas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.josjis.maracas.R

@Composable
fun BatteryOptimizationButton(
    masterOn: Boolean,
    isBatteryUnrestricted: Boolean,
    onRequestUnrestrictedBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = masterOn && !isBatteryUnrestricted
    val textRes = if (isBatteryUnrestricted) R.string.battery_granted else R.string.action_battery

    OutlinedButton(
        onClick = onRequestUnrestrictedBattery,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_battery),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
