@file:OptIn(ExperimentalMaterial3Api::class)

package me.marthia.slider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ertebatbonyan.android.base.R
import com.ertebatbonyan.android.base.app.theme.AppColors
import com.ertebatbonyan.android.base.app.theme.AppTypography
import timber.log.Timber


@Composable
fun SliderWithLabel(
    modifier: Modifier = Modifier,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> String?,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,

    ) {

    val labelText = remember { mutableStateOf("${value.toInt()}") }
    val changedValue = remember { mutableFloatStateOf(value) }
    var showLabel by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {

        BoxWithConstraints {

            if (showLabel) {
                val offset = getSliderOffset(
                    value = value,
                    valueRange = valueRange,
                    boxWidth = maxWidth,
                    labelWidth = 24.dp + 8.dp // Since we use a padding of 4.dp on either sides of the SliderLabel, we need to account for this in our calculation
                )

                SliderLabel(
                    label = labelText.value, modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(2f)
                        .offset(x = offset, y = (-12).dp)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = { positionDs ->
                Timber.d("Slider Internals: position changed $positionDs")
                changedValue.value = positionDs
                showLabel = true
                onValueChange(positionDs)?.let { currentGroupValue ->
                    labelText.value = currentGroupValue
                }
            },
            valueRange = valueRange,
            modifier = modifier.fillMaxWidth(),
            onValueChangeFinished = {
                Timber.d("Slider Internals: user interaction finished with value ${labelText.value}")
                showLabel = false
                onValueChangeFinished?.invoke(changedValue.value)
            },
            colors = colors,
            enabled = enabled,
            thumb = {},
            track = { sliderState ->
                WaveFormTrack(modifier = Modifier, sliderState = sliderState)
            },
        )
    }

    LaunchedEffect(value) {
        Timber.d("Slider Internals: base value updated $value")
    }
}

@Composable
fun WaveFormTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = color.copy(alpha = 0.25f),
    progressMaxHeight: Dp = 32.dp,
    numberOfSegments: Int = 60,
    segmentGap: Dp = 2.dp
) {
    if (sliderState.valueRange.endInclusive <= 0) {
        Timber.d("Invalid range: Skipping")
        return
    }

    val progress = sliderState.value / sliderState.valueRange.endInclusive

    Timber.d("progress = $progress")

    if (progress !in 0f..1f) {
        Timber.d("Invalid progress $progress")
        return
    }
    if (numberOfSegments <= 0) {
        Timber.d("Number of segments must be greater than 1")
        return
    }

    val gap: Float
    val barHeight: Float
    with(LocalDensity.current) {
        gap = segmentGap.toPx()
        barHeight = progressMaxHeight.toPx()
    }

    Canvas(
        modifier
            .progressSemantics(progress)
            .fillMaxWidth()
    ) {
        drawSegments(1f, backgroundColor, barHeight, numberOfSegments, gap)
        drawSegments(progress, color, barHeight, numberOfSegments, gap)
    }
}

private fun DrawScope.drawSegments(
    progress: Float,
    color: Color,
    segmentHeight: Float,
    segments: Int,
    segmentGap: Float,
) {
    val width = size.width
    val start = 0f
    val gaps = (segments - 1) * segmentGap
    val segmentWidth = (width - gaps) / segments
    val barsWidth = segmentWidth * segments
    // 1
    val end = barsWidth * progress + (progress * segments).toInt() * segmentGap

    repeat(segments) { index ->

        // pseudo noise to randomize bar height
        val factor: Float = when {
            index == 0 -> 0.05f
            index % 10 == 0 -> 0.9f
            index % 4 == 0 -> 0.25f
            index % 4 == 1 -> 0.50f
            index % 4 == 2 -> 0.75f
            index % 4 == 3 -> 0.50f
            else -> 1f

        }
        val offset = index * (segmentWidth + segmentGap)
        if (offset < end) {
            val segmentEnd = (offset + segmentWidth).coerceAtMost(end)
            val segmentStart = start + offset
            val height = segmentHeight * factor
            drawRoundRect(
                color = color,
                topLeft = Offset(segmentStart, -(height / 2)),
                cornerRadius = CornerRadius(x = barsWidth / 2),
                size = Size(segmentEnd - segmentStart, height)
            )
        }
    }
}

@Composable
fun SliderLabel(label: String, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .paint(
                painter = painterResource(id = R.drawable.slider_tooltip),
                colorFilter = ColorFilter.tint(AppColors.primary)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = AppColors.onPrimary,
            maxLines = 1,
            style = AppTypography.labelSmall,
        )
    }
}

class CustomShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            arcTo(
                rect = Rect(
                    topLeft = Offset(size.width / 2f, 0f),
                    bottomRight = Offset(size.width / 2f, size.height)
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
            moveTo(size.width / 2f, 0f)
            arcTo(
                rect = Rect(
                    topLeft = Offset(size.width / 2f, 0f),
                    bottomRight = Offset(size.width / 2f, size.height)
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
            close()
        }
        return Outline.Generic(path)
    }
}


private fun getSliderOffset(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    boxWidth: Dp,
    labelWidth: Dp
): Dp {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val positionFraction = calcFraction(valueRange.start, valueRange.endInclusive, coerced)

    return (boxWidth - labelWidth) * positionFraction
}


// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SliderWithLabelPreview() {
    SliderWithLabel(
        value = 5F,
        valueRange = 0F..100F,
        onValueChange = {
            "Value=$it"
        })
}
