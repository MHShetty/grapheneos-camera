package app.grapheneos.camera.ui.composable.screen.navtype

import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat

import androidx.navigation.NavType
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ui.composable.screen.serializer.CapturedItemSerializer
import kotlinx.serialization.json.Json

object CapturedItemNavType : NavType<CapturedItem?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): CapturedItem? {
        val item = BundleCompat.getParcelable(bundle, key, CapturedItem::class.java)
        return item
    }

    override fun parseValue(value: String): CapturedItem? {
        if (value.isEmpty()) return null
        return Json.decodeFromString(CapturedItemSerializer, Uri.decode(value))
    }

    override fun serializeAsValue(value: CapturedItem?): String {
        if (value == null) return ""
        return Uri.encode(Json.encodeToString(CapturedItemSerializer, value))
    }

    override fun put(bundle: Bundle, key: String, value: CapturedItem?) {
        bundle.putParcelable(key, value)
    }
}