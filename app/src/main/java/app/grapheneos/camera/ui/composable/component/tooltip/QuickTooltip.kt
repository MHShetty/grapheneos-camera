package app.grapheneos.camera.ui.composable.component.tooltip

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import app.grapheneos.camera.ktx.toPx
import kotlin.math.absoluteValue

private const val TAG = "QuickTooltip"

enum class QuickTooltipVerticalDirection {
    TOP,
    BOTTOM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTooltip(
    message: String,
    tooltipAnchorVerticalSpacing: Float = 16.dp.toPx(),
    defaultDirection: QuickTooltipVerticalDirection = QuickTooltipVerticalDirection.BOTTOM,
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    val tooltipState = rememberTooltipState()

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            hapticFeedback.performHapticFeedback(
                HapticFeedbackType.LongPress
            )
        }
    }

    TooltipBox (
        positionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {

                val anchorPopWidthDiff = anchorBounds.width - popupContentSize.width

                val xOffset = if (anchorPopWidthDiff >= 0) {
                    anchorBounds.left + anchorPopWidthDiff / 2
                } else if (
                    (windowSize.width - anchorBounds.right >= anchorPopWidthDiff.absoluteValue / 2)
                ) {
                    anchorBounds.left - anchorPopWidthDiff.absoluteValue / 2
                } else {
                    anchorBounds.right - popupContentSize.width
                }.toInt()

                var yOffset : Float

                if (defaultDirection == QuickTooltipVerticalDirection.TOP) {
                    yOffset = anchorBounds.top - popupContentSize.height - tooltipAnchorVerticalSpacing
                    if (yOffset < 0)
                        yOffset = anchorBounds.bottom + tooltipAnchorVerticalSpacing
                } else {
                    yOffset = anchorBounds.bottom + tooltipAnchorVerticalSpacing
                }

                return IntOffset(xOffset, yOffset.toInt())
            }
        },

        tooltip = {
            PlainTooltip {
                Text(
                    message,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(
                            vertical = 4.dp,
                            horizontal = 2.dp,
                        )
                )
            }

        },
        state = tooltipState,
        content = content
    )
}