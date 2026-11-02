package id.josjis.maracas.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dottedBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color,
    cornerRadius: Dp = 24.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 6.dp
): Modifier = this.drawWithContent {
    drawContent()
    val strokeWidthPx = strokeWidth.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    val cornerRadiusPx = cornerRadius.toPx()

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = strokeWidthPx / 2f,
                top = strokeWidthPx / 2f,
                right = size.width - strokeWidthPx / 2f,
                bottom = size.height - strokeWidthPx / 2f,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
        )
    )
}
