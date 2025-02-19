package app.grapheneos.camera.util

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ITEM_TYPE_VIDEO

const val VIDEO_MIME_TYPE = "video/mp4"
const val IMAGE_MIME_TYPE = "image/jpg"
const val GENERIC_MIME_TYPE = "*/*"

fun getMimeTypeForItems(items: List<CapturedItem>) : String {

    if (items.isEmpty()) return GENERIC_MIME_TYPE

    var hasPhoto = false
    var hasVideo = false

    for (item in items) {
        if (item.type == ITEM_TYPE_VIDEO) {
            hasVideo = true
            if (hasPhoto) return GENERIC_MIME_TYPE
        } else {
            hasPhoto = true
            if (hasVideo) return GENERIC_MIME_TYPE
        }
    }

    if (hasVideo) {
        return VIDEO_MIME_TYPE
    } else {
        return IMAGE_MIME_TYPE
    }
}