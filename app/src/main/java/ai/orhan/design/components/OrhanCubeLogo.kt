package ai.orhan.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Orhan's official Cube logo — a modern 3D isometric geometric cube.
 *
 * @param modifier Modifier applied to the outer container.
 * @param size Display size of the cube.
 * @param strokeColor Color for the cube's wireframe edges. Defaults to primary color.
 * @param filled Whether to render subtle shaded faces for depth.
 */
@Composable
fun OrhanCubeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = true,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.width * 0.44f
            val side = r * 0.866f

            // Vertex coordinates
            val topX = cx
            val topY = cy - r
            val topRX = cx + side
            val topRY = cy - (r * 0.5f)
            val botRX = cx + side
            val botRY = cy + (r * 0.5f)
            val botY = cy + r
            val botLX = cx - side
            val botLY = cy + (r * 0.5f)
            val topLX = cx - side
            val topLY = cy - (r * 0.5f)

            if (filled) {
                // Top face
                val topFace = Path().apply {
                    moveTo(topX, topY)
                    lineTo(topRX, topRY)
                    lineTo(cx, cy)
                    lineTo(topLX, topLY)
                    close()
                }
                drawPath(
                    path = topFace,
                    brush = Brush.verticalGradient(
                        listOf(strokeColor.copy(alpha = 0.22f), strokeColor.copy(alpha = 0.10f)),
                        startY = topY,
                        endY = cy,
                    ),
                    style = Fill,
                )

                // Left face
                val leftFace = Path().apply {
                    moveTo(topLX, topLY)
                    lineTo(cx, cy)
                    lineTo(cx, botY)
                    lineTo(botLX, botLY)
                    close()
                }
                drawPath(
                    path = leftFace,
                    color = strokeColor.copy(alpha = 0.16f),
                    style = Fill,
                )

                // Right face
                val rightFace = Path().apply {
                    moveTo(cx, cy)
                    lineTo(topRX, topRY)
                    lineTo(botRX, botRY)
                    lineTo(cx, botY)
                    close()
                }
                drawPath(
                    path = rightFace,
                    color = strokeColor.copy(alpha = 0.08f),
                    style = Fill,
                )
            }

            // Outline
            val outline = Path().apply {
                moveTo(topX, topY)
                lineTo(topRX, topRY)
                lineTo(botRX, botRY)
                lineTo(cx, botY)
                lineTo(botLX, botLY)
                lineTo(topLX, topLY)
                close()
            }

            // Inner dividing edges
            val innerEdges = Path().apply {
                moveTo(topLX, topLY); lineTo(cx, cy)
                moveTo(topRX, topRY); lineTo(cx, cy)
                moveTo(cx, botY); lineTo(cx, cy)
            }

            val strokeWidthPx = (size.value * 0.08f).coerceIn(1.5f, 3.5f).dp.toPx()
            val strokeStyle = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )

            drawPath(path = outline, color = strokeColor, style = strokeStyle)
            drawPath(path = innerEdges, color = strokeColor, style = strokeStyle)
        }
    }
}

@Preview(name = "Orhan Cube Logo", showBackground = true)
@Composable
private fun OrhanCubeLogoPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OrhanCubeLogo(size = 48.dp)
        }
    }
}
