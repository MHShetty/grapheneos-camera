package app.grapheneos.camera.ui.composable.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

import app.grapheneos.camera.ui.composable.screen.serializer.VideoUriSerializer

import kotlinx.serialization.Serializable

@Serializable(with = VideoUriSerializer::class)
data class VideoUri(
    val uri: Uri
) : Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        uri.writeToParcel(dest, flags)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<VideoUri> {
            override fun createFromParcel(source: Parcel): VideoUri {
                val uri = Uri.CREATOR.createFromParcel(source)
                return VideoUri(uri)
            }

            override fun newArray(size: Int) = arrayOfNulls<VideoUri>(size)
        }
    }
}