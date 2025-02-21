package app.grapheneos.camera.ui.composable.component.mediapreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.R
import coil3.compose.AsyncImagePainter

private const val TAG = "MediaPreviewStateShowcase"

enum class MediaPreviewLoaderType {
    THREE_DOTS,
    SQUARE_BOX,
}

enum class MediaPreviewErrorType {
    INFO_MESSAGE,
    ERROR_ICON,
}

@Composable
fun MediaPreviewStateShowcase(
    painter: AsyncImagePainter,
    capturedItem: CapturedItem,
    mediaPreviewLoaderType : MediaPreviewLoaderType = MediaPreviewLoaderType.THREE_DOTS,
    mediaPreviewErrorType: MediaPreviewErrorType = MediaPreviewErrorType.INFO_MESSAGE,
) {
    val imageState by painter.state.collectAsStateWithLifecycle()

    if (imageState is AsyncImagePainter.State.Empty || imageState is AsyncImagePainter.State.Loading) {

        if (mediaPreviewLoaderType == MediaPreviewLoaderType.THREE_DOTS) {
            Text(
                text = stringResource(R.string.three_dots),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )
        } else {
            Box(
                modifier = Modifier
                    .background(Color.Gray)
                    .aspectRatio(1f)
                    .fillMaxSize()
            )
        }




    } else if (imageState is AsyncImagePainter.State.Error) {
        if (mediaPreviewErrorType == MediaPreviewErrorType.INFO_MESSAGE) {
            Text(
                text = stringResource(R.string.load_media_failure_message, capturedItem.uiName()),
                color = Color.Gray,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .aspectRatio(1f)
                    .fillMaxSize(),
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = stringResource(R.string.error),
                )
            }

        }
    }
}