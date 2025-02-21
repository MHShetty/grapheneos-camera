package app.grapheneos.camera.ui.composable.component.mediapreview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.R

import coil3.ImageLoader

import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder

private const val TAG = "MediaPreview"

@Composable
fun MediaPreview(
    capturedItem: CapturedItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            .build()
    }

    val imagePainter = rememberAsyncImagePainter(
        model = imageRequest,
        imageLoader = imageLoader,
    )

    // Update image on resume
    // Use case: When user edits the original image and returns back to the app
    LifecycleResumeEffect(Unit) {
        imagePainter.restart()
        onPauseOrDispose {}
    }

    MediaPreviewStateShowcase(
        painter = imagePainter,
        capturedItem = capturedItem,
        mediaPreviewLoaderType = MediaPreviewLoaderType.THREE_DOTS,
        mediaPreviewErrorType = MediaPreviewErrorType.INFO_MESSAGE
    )

    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = imagePainter,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
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
                        shape = CircleShape,
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color(0x50ffffff),
                        shape = CircleShape,
                    )
                    .requiredSize(56.dp)
                    .padding(12.dp)
                    .align(Alignment.Center)
            )
        }
    }
}