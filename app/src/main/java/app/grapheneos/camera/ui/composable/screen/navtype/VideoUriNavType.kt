package app.grapheneos.camera.ui.composable.screen.navtype

import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.navigation.NavType
import app.grapheneos.camera.ui.composable.model.VideoUri
import app.grapheneos.camera.ui.composable.screen.serializer.VideoUriSerializer
import kotlinx.serialization.json.Json

object VideoUriNavType : NavType<VideoUri>(false) {

    override fun get(bundle: Bundle, key: String): VideoUri? {
        return BundleCompat.getParcelable(bundle, key, VideoUri::class.java)
    }

    override fun put(bundle: Bundle, key: String, value: VideoUri) {
        bundle.putParcelable(key, value)
    }

    override fun parseValue(value: String): VideoUri {
        return Json.decodeFromString(VideoUriSerializer, Uri.decode(value))
    }

    override fun serializeAsValue(value: VideoUri): String {
        return Uri.encode(Json.encodeToString(VideoUriSerializer, value))
    }
}