package app.grapheneos.camera.ui.composable.screen.ui

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.grapheneos.camera.R
import app.grapheneos.camera.ui.composable.theme.AppColor
import kotlinx.coroutines.launch

private const val TAG = "VideoPlayerScreen"

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    mediaUri: Uri,
    onExitAction: () -> Unit = {},
) {

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Executed once (unless the mediaUri the key here changes)
    LaunchedEffect(mediaUri) {
        coroutineScope.launch {
            var hasAudio = true
            try {
                MediaMetadataRetriever().use {
                    it.setDataSource(context, mediaUri)
                    hasAudio = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
                }
            } catch (e: Exception) {
                Log.d(TAG, "Unable to retrieve HAS_AUDIO metadata for video", e)
            }

            // Requests for focus when audio is enabled
            if (hasAudio) {
                exoPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
            }

            val mediaItem = MediaItem.fromUri(mediaUri)
            exoPlayer.setMediaItem(mediaItem)

            exoPlayer.prepare()

            // Auto-play only when the player first starts
            exoPlayer.play()
        }

    }

    LifecycleResumeEffect(Unit) {
        onPauseOrDispose {
            // Don't play when user is away from screen
            if (exoPlayer.isPlaying)
                exoPlayer.pause()
        }
    }

    // Run only once, a listener set which
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Main UI
    Scaffold (
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarColors(
                    containerColor = AppColor.AppBarColor,
                    scrolledContainerColor = AppColor.AppBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                navigationIcon = {
                    IconButton(onClick = onExitAction) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        containerColor = AppColor.BackgroundColor,
        content = { innerPadding ->
            AndroidView(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),

                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        controllerShowTimeoutMs = 1200
                    }
                }
            )

        }
    )
}
