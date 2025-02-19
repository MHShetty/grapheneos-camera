package app.grapheneos.camera.ui.composable.component.mediapreview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.motion.widget.MotionScene.Transition.TransitionOnClick
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.R
import app.grapheneos.camera.ktx.toPx
import coil3.ImageLoader
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder

private const val TAG = "SquareMediaPreview"

val SQUARE_MEDIA_PREVIEW_SIZE = 80.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquareMediaPreview(
    capturedItem: CapturedItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    imageSize: Int = SQUARE_MEDIA_PREVIEW_SIZE.toPx().toInt(),
) {
    val context = LocalContext.current

    val haptic = LocalHapticFeedback.current

    val imageLoader = remember(context, capturedItem) {
        ImageLoader.Builder(context)
            .components {
                if (capturedItem.type == ITEM_TYPE_VIDEO) {
                    add(VideoFrameDecoder.Factory())
                }
            }
            .build()
    }

    val imageRequest = remember(capturedItem) {
        ImageRequest.Builder(context)
            .data(capturedItem.uri)
            .size(imageSize)
            .build()
    }

    val imagePainter = rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = imageLoader,
    )

    MediaPreviewStateShowcase(
        painter = imagePainter,
        capturedItem = capturedItem,
        mediaPreviewLoaderType = MediaPreviewLoaderType.SQUARE_BOX,
        mediaPreviewErrorType = MediaPreviewErrorType.ERROR_ICON,
    )

    Box(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    ) {
        Image(
            painter = imagePainter,
            modifier = modifier
                .aspectRatio(1f)
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentDescription = stringResource(R.string.preview)
        )

        if (capturedItem.type == ITEM_TYPE_VIDEO) {
            Icon(
                painter = painterResource(R.drawable.play),
                tint = Color.White,
                contentDescription = stringResource(R.string.play_video),
                modifier = modifier
                    .background(
                        color = Color(0x99000000),
                        shape = CircleShape
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color(0x50ffffff),
                        shape = CircleShape
                    )
                    .requiredSize(48.dp)
                    .padding(12.dp)
                    .align(Alignment.Center)
            )
        }
    }
}