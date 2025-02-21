package app.grapheneos.camera.ui.composable.screen.routes

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ui.composable.screen.navtype.CapturedItemNavType
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable

//@Serializable
//object GalleryRoute

@Serializable
data class GalleryRoute(
    val focusItem: CapturedItem? = null
) {
    companion object {
        val typeMap = mapOf(
            typeOf<CapturedItem?>() to CapturedItemNavType,
        )
    }
}