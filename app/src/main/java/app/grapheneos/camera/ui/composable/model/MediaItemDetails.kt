package app.grapheneos.camera.ui.composable.model

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.provider.OpenableColumns
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidxc.exifinterface.media.ExifInterface
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.R
import app.grapheneos.camera.util.storageLocationToUiString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MediaItemDetails(
    val filePath: String?,
    val fileName: String?,
    val size: String?,
    var dateAdded: String?,
    var dateModified: String?,
) {

    companion object {
        val Saver = Saver<MutableState<MediaItemDetails?>, Array<String?>>(
            save = {
                val item = it.value ?: return@Saver arrayOf()
                arrayOf(item.filePath, item.fileName, item.size, item.dateAdded, item.dateModified)
            },
            restore = {
                if (it.isEmpty()) return@Saver null
                mutableStateOf(MediaItemDetails(it[0], it[1], it[2], it[3], it[4]))
            },
        )

        fun forCapturedItem(context: Context, item: CapturedItem) : MediaItemDetails {

            var relativePath: String? = null
            var fileName: String? = null
            var size: String? = null

            var dateAdded: String? = null
            var dateModified: String? = null

            val projection = arrayOf(MediaColumns.RELATIVE_PATH, OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

            context.contentResolver.query(item.uri, projection, null,null)?.use {
                if (it.moveToFirst()) {
                    fileName = it.getString(1)

                    if (fileName == null) {
                        throw Exception("File name not found for file")
                    }

                    relativePath = getRelativePath(context, item.uri, it.getString(0), fileName!!)
                    size = String.format(Locale.ROOT, "%.2f MB", (it.getLong(2) / (1000f * 1000f)))
                }
            }

            if (item.type == ITEM_TYPE_VIDEO) {
                MediaMetadataRetriever().use {
                    it.setDataSource(context, item.uri)
                    dateAdded = convertTimeForVideo(it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)!!)
                    dateModified = dateAdded

                }
            } else {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    val eInterface = ExifInterface(stream)

                    val offset = eInterface.getAttribute(ExifInterface.TAG_OFFSET_TIME)

                    if (eInterface.hasAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)) {
                        dateAdded = convertTimeForPhoto(
                            eInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)!!,
                            offset
                        )
                    }

                    if (eInterface.hasAttribute(ExifInterface.TAG_DATETIME)) {
                        dateModified = convertTimeForPhoto(
                            eInterface.getAttribute(ExifInterface.TAG_DATETIME)!!,
                            offset
                        )
                    }
                }
            }

            return MediaItemDetails(
                relativePath,
                fileName,
                size,
                dateAdded,
                dateModified,
            )
        }

        @SuppressLint("SimpleDateFormat")
        fun convertTime(time: Long, showTimeZone: Boolean = true): String {
            val date = Date(time)
            val format = SimpleDateFormat(
                if (showTimeZone) {
                    "yyyy-MM-dd HH:mm:ss z"
                } else {
                    "yyyy-MM-dd HH:mm:ss"
                }
            )
            format.timeZone = TimeZone.getDefault()
            return format.format(date)
        }

        private fun convertTimeForVideo(time: String): String {
            val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val parsedDate = dateFormat.parse(time)
            return convertTime(parsedDate?.time ?: 0)
        }

        private fun convertTimeForPhoto(time: String, offset: String? = null): String {
            val timestamp = if (offset != null) {
                "$time $offset"
            } else {
                time
            }

            val dateFormat = SimpleDateFormat(
                if (offset == null) {
                    "yyyy:MM:dd HH:mm:ss"
                } else {
                    "yyyy:MM:dd HH:mm:ss Z"
                }, Locale.US
            )

            if (offset == null) {
                dateFormat.timeZone = TimeZone.getDefault()
            }
            val parsedDate = dateFormat.parse(timestamp)
            return convertTime(parsedDate?.time ?: 0, offset != null)
        }

        private fun getRelativePath(ctx: Context, uri: Uri, path: String?, fileName: String): String {
            if (path == null) {
                return storageLocationToUiString(ctx, uri.toString())
            }

            return "${ctx.getString(R.string.main_storage)}/$path$fileName"
        }
    }
}