package app.grapheneos.camera.ui.composable.screen.routes

import app.grapheneos.camera.ui.composable.model.VideoUri
import app.grapheneos.camera.ui.composable.screen.navtype.VideoUriNavType

import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable

@Serializable
data class VideoPlayerRoute(
    val videoUri: VideoUri,
) {
    companion object {
        val typeMap = mapOf(
            typeOf<VideoUri>() to VideoUriNavType,
        )
    }
}

