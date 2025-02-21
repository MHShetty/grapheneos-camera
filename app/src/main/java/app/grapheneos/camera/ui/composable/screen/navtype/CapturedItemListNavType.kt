package app.grapheneos.camera.ui.composable.screen.navtype

import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.navigation.NavType
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ui.composable.screen.serializer.CapturedItemSerializer
import kotlinx.serialization.json.Json

object CapturedItemListNavType : NavType<List<CapturedItem>?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): List<CapturedItem>? {
        return BundleCompat.getParcelableArrayList(bundle, key, CapturedItem::class.java)
    }

    override fun parseValue(value: String): List<CapturedItem>? {
        if (value.isEmpty()) return null
        return Json.decodeFromString(CapturedItemSerializer.ListSerializer, Uri.decode(value))
    }

    override fun serializeAsValue(value: List<CapturedItem>?): String {
        if (value == null) return ""
        return Uri.encode(Json.encodeToString(CapturedItemSerializer.ListSerializer, value))
    }

    override fun put(bundle: Bundle, key: String, value: List<CapturedItem>?) {
        if (value != null)
            bundle.putParcelableArrayList(key, ArrayList(value))
    }

}