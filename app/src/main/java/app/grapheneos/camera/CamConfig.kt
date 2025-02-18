package app.grapheneos.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import app.grapheneos.camera.analyzer.QRAnalyzer
import app.grapheneos.camera.ktx.markAs16by9Layout
import app.grapheneos.camera.ktx.markAs4by3Layout
import app.grapheneos.camera.ui.SettingsDialog
import app.grapheneos.camera.ui.activities.CaptureActivity
import app.grapheneos.camera.ui.activities.MainActivity
import app.grapheneos.camera.ui.activities.MoreSettings
import app.grapheneos.camera.ui.activities.SecureActivity
import app.grapheneos.camera.ui.activities.SecureMainActivity
import app.grapheneos.camera.ui.activities.VideoCaptureActivity
import app.grapheneos.camera.ui.activities.VideoOnlyActivity
import app.grapheneos.camera.ui.showIgnoringShortEdgeMode
import app.grapheneos.camera.util.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

// note that enum constant name is used as a name of a SharedPreferences instance
enum class CameraMode(val extensionMode: Int, val uiName: Int) {
    QR_SCAN(ExtensionMode.NONE, R.string.qr_scan_mode),
    AUTO(ExtensionMode.AUTO, R.string.auto_mode),
    FACE_RETOUCH(ExtensionMode.FACE_RETOUCH, R.string.face_retouch_mode),
    PORTRAIT(ExtensionMode.BOKEH, R.string.portrait_mode),
    NIGHT(ExtensionMode.NIGHT, R.string.night_mode),
    HDR(ExtensionMode.HDR, R.string.hdr_mode),
    CAMERA(ExtensionMode.NONE, R.string.camera),
    VIDEO(ExtensionMode.NONE, R.string.video),
}

@SuppressLint("UnsafeOptInUsageError")
class CamConfig(private val mActivity: MainActivity) {

    enum class GridType {
        NONE,
        THREE_BY_THREE,
        FOUR_BY_FOUR,
        GOLDEN_RATIO
    }

    object SettingValues {

        object Key {
            const val SELF_ILLUMINATION = "self_illumination"
            const val GEO_TAGGING = "geo_tagging"
            const val FLASH_MODE = "flash_mode"
            const val GRID = "grid"
            const val EMPHASIS_ON_QUALITY = "emphasis_on_quality"
            const val FOCUS_TIMEOUT = "focus_timeout"
            const val VIDEO_QUALITY = "video_quality"
            const val VIDEO_FRAME_RATE = "video_frame_rate"
            const val ASPECT_RATIO = "aspect_ratio"
            const val INCLUDE_AUDIO = "include_audio"
            const val ENABLE_EIS = "enable_eis"
            const val SCAN = "scan"
            const val SCAN_ALL_CODES = "scan_all_codes"
            const val SAVE_IMAGE_AS_PREVIEW = "save_image_as_preview"
            const val SAVE_VIDEO_AS_PREVIEW = "save_video_as_preview"

            const val STORAGE_LOCATION = "storage_location"
            const val PREVIOUS_SAF_TREES = "previous_saf_trees"

            const val LAST_CAPTURED_ITEM_TYPE = "last_captured_item_type"
            const val LAST_CAPTURED_ITEM_DATE_STRING = "last_captured_item_date_string"
            const val LAST_CAPTURED_ITEM_URI = "last_captured_item_uri"

            const val PHOTO_QUALITY = "photo_quality"

            const val REMOVE_EXIF_AFTER_CAPTURE = "remove_exif_after_capture"

            const val GYROSCOPE_SUGGESTIONS = "gyroscope_suggestions"

            const val CAMERA_SOUNDS = "camera_sounds"

            const val ENABLE_ZSL = "enable_zsl"

            // const val IMAGE_FILE_FORMAT = "image_quality"
            // const val VIDEO_FILE_FORMAT = "video_quality"
        }

        object Default {

            val GRID_TYPE = GridType.NONE
            const val GRID_TYPE_INDEX = 0

            const val ASPECT_RATIO = AspectRatio.RATIO_4_3

            val VIDEO_QUALITY = Quality.HIGHEST

            val VIDEO_FRAME_RATE = Range<Int>(30, 30)

            const val SELF_ILLUMINATION = false

            const val GEO_TAGGING = false

            const val FLASH_MODE = ImageCapture.FLASH_MODE_OFF

            const val EMPHASIS_ON_QUALITY = false

            const val FOCUS_TIMEOUT = "5s"

            const val INCLUDE_AUDIO = true

            const val ENABLE_EIS = true

            const val SCAN_ALL_CODES = false

            const val SAVE_IMAGE_AS_PREVIEW = true

            const val SAVE_VIDEO_AS_PREVIEW = true

            const val STORAGE_LOCATION = ""

            const val PHOTO_QUALITY = 0

            const val REMOVE_EXIF_AFTER_CAPTURE = true

            const val GYROSCOPE_SUGGESTIONS = false

            const val CAMERA_SOUNDS = true

            const val ENABLE_ZSL = false

            // const val IMAGE_FILE_FORMAT = ""
            // const val VIDEO_FILE_FORMAT = ""
        }
    }

    companion object {
        private const val TAG = "CamConfig"

        private const val PREVIEW_SNAP_DURATION = 200L
        private const val PREVIEW_SL_OVERLAY_DUR = 200L

        const val DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_BACK

        val commonFormats = arrayOf(
            BarcodeFormat.AZTEC,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.PDF_417,
        )

        val imageCollectionUri: Uri = MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )!!

        val videoCollectionUri: Uri = MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )!!

        val DEFAULT_CAMERA_MODE = CameraMode.CAMERA

        const val COMMON_SHARED_PREFS_NAME = "commons"

        val FRONT_CAMERA_SELECTOR = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val REAR_CAMERA_SELECTOR = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    var camera: Camera? = null

    var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null

    var imageCapture: ImageCapture? = null
        private set

    private var preview: Preview? = null

    val allowedFormats: ArrayList<BarcodeFormat> = arrayListOf()

    private val cameraExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    var videoCapture: VideoCapture<Recorder>? = null

    private var qrAnalyzer: QRAnalyzer? = null

    var iAnalyzer: ImageAnalysis? = null

    val mPlayer = TunePlayer(mActivity)

    // note that Activities which implement SecureActivity interface (meaning they are accessible
    // from the lock screen) are forced to override getSharedPreferences()
    // and return an instance of in-memory EphemeralSharedPrefs, which are based on "real" prefs,
    // but never modify them
    val commonPref: SharedPreferences = mActivity.getSharedPreferences(COMMON_SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    private lateinit var modePref: SharedPreferences

    var lastCapturedItem: CapturedItem? = null

    init {
        if (mActivity !is SecureActivity) {
            CapturedItems.init(mActivity, this)
            fetchLastCapturedItemFromSharedPrefs()
        }
    }

    fun fetchLastCapturedItemFromSharedPrefs() {
        val type = commonPref.getInt(SettingValues.Key.LAST_CAPTURED_ITEM_TYPE, -1)
        val dateStr = commonPref.getString(SettingValues.Key.LAST_CAPTURED_ITEM_DATE_STRING, null)
        val uri = commonPref.getString(SettingValues.Key.LAST_CAPTURED_ITEM_URI, null)

        var item: CapturedItem? = null
        if (dateStr != null && uri != null) {
            val skip = type == ITEM_TYPE_IMAGE && mActivity is VideoOnlyActivity
            if (!skip) {
                item = CapturedItem(type, dateStr, Uri.parse(uri))
            }
        }
        lastCapturedItem = item
    }


    var isVideoMode = false
        private set
        get() {
            return field ||
                    mActivity is VideoCaptureActivity ||
                    mActivity is VideoOnlyActivity
        }

    val canTakePicture : Boolean
        get() {
            return imageCapture != null
        }

    var isQRMode = false
        private set

    val isFlashAvailable: Boolean
        get() = camera?.cameraInfo?.hasFlashUnit() ?: false

    var isTorchOn: Boolean = false
        get() {
            return camera?.cameraInfo?.torchState?.value == TorchState.ON
        }
        set(value) {
            field = if (isFlashAvailable) {
                camera?.cameraControl?.enableTorch(value)
                value
            } else {
                false
            }
        }

    private var currentMode: CameraMode = DEFAULT_CAMERA_MODE

    var aspectRatio: Int
        get() {
            return when {
                isVideoMode -> {
                    AspectRatio.RATIO_16_9
                }
                isQRMode -> {
                    AspectRatio.RATIO_4_3
                }
                else -> {
                    commonPref.getInt(
                        SettingValues.Key.ASPECT_RATIO,
                        SettingValues.Default.ASPECT_RATIO
                    )
                }
            }
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putInt(SettingValues.Key.ASPECT_RATIO, value)
            editor.apply()
        }

    var lensFacing = DEFAULT_LENS_FACING

    private var cameraSelector: CameraSelector = CameraSelector.Builder()
        .requireLensFacing(DEFAULT_LENS_FACING)
        .build()

    var gridType: GridType = SettingValues.Default.GRID_TYPE
        set(value) {
            val editor = commonPref.edit()
            editor.putInt(SettingValues.Key.GRID, GridType.values().indexOf(value))
            editor.apply()

            field = value
        }

    var videoQuality: Quality = SettingValues.Default.VIDEO_QUALITY
        get() {
            return if (modePref.contains(videoQualityKey)) {
                SettingsDialog.titleToQuality(
                    modePref.getString(videoQualityKey, "")!!
                )
            } else {
                SettingValues.Default.VIDEO_QUALITY
            }
        }
        set(value) {
            val option = mActivity.settingsDialog.videoQualitySpinner.selectedItem as String

            modePref.edit {
                putString(videoQualityKey, option)
            }

            field = value
        }

    private val videoQualityKey: String
        get() {

            val pf = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                "FRONT"
            } else {
                "BACK"
            }

            return "${SettingValues.Key.VIDEO_QUALITY}_$pf"
        }

    var videoFrameRate: Range<Int> = SettingValues.Default.VIDEO_FRAME_RATE
        get() {
            return if (modePref.contains(videoFrameRateKey)) {
                SettingsDialog.titleToFrameRateRange(
                    modePref.getString(videoFrameRateKey, "")!!
                )
            } else {
                defaultVideoFrameRate
            }
        }
        set(value) {
            modePref.edit {
                putString(videoFrameRateKey, mActivity.settingsDialog.getTitleForFrameRateRange(value))
            }

            field = value
        }

    val videoFrameRateKey : String
        get() {
            val pf = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                "FRONT"
            } else {
                "BACK"
            }

            return "${SettingValues.Key.VIDEO_FRAME_RATE}_$pf"
        }

    val defaultVideoFrameRate : Range<Int>
        get() {
            val availableFrameRates = getAvailableVideoFrameRates()
            if (availableFrameRates.contains(SettingValues.Default.VIDEO_FRAME_RATE))
                return SettingValues.Default.VIDEO_FRAME_RATE
            return availableFrameRates[0]
        }

    var flashMode: Int
        get() = if (imageCapture != null) imageCapture!!.flashMode else
            SettingValues.Default.FLASH_MODE
        set(flashMode) {

            if (::modePref.isInitialized) {
                modePref.edit {
                    putInt(SettingValues.Key.FLASH_MODE, flashMode)
                }
            }

            imageCapture?.flashMode = flashMode
            mActivity.settingsDialog.updateFlashMode()
        }

    var focusTimeout = 5L
        set(value) {
            val option = if (value == 0L) {
                "Off"
            } else {
                "${value}s"
            }

            val editor = commonPref.edit()
            editor.putString(SettingValues.Key.FOCUS_TIMEOUT, option)
            editor.apply()

            field = value
        }

    var enableCameraSounds: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.CAMERA_SOUNDS,
                SettingValues.Default.CAMERA_SOUNDS
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.CAMERA_SOUNDS, value)
            editor.apply()
        }

    var scanAllCodes: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.SCAN_ALL_CODES,
                SettingValues.Default.SCAN_ALL_CODES
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.SCAN_ALL_CODES, value)
            editor.apply()

            if (isQRMode) {
                if (value) {
                    mActivity.flipCamIcon.setImageResource(
                        R.drawable.cancel
                    )
                    mActivity.qrScanToggles.visibility = View.GONE
                } else {
                    mActivity.flipCamIcon.setImageResource(
                        R.drawable.auto
                    )
                    mActivity.qrScanToggles.visibility = View.VISIBLE
                }
            }

            qrAnalyzer?.refreshHints()
        }

    var includeAudio: Boolean
        get() {
            return mActivity.settingsDialog.includeAudioToggle.isChecked
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.INCLUDE_AUDIO, value)
            editor.apply()

            mActivity.settingsDialog.includeAudioToggle.isChecked = value
        }

    var enableEIS: Boolean
        get() {
            return isVideoStabilizationSupported() && mActivity.settingsDialog.enableEISToggle.isChecked
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.ENABLE_EIS, value)
            editor.apply()

            mActivity.settingsDialog.enableEISToggle.isChecked = value
        }

    var enableZsl: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.ENABLE_ZSL,
                SettingValues.Default.ENABLE_ZSL
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.ENABLE_ZSL, value)
            editor.apply()
        }

    var saveImageAsPreviewed: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.SAVE_IMAGE_AS_PREVIEW,
                SettingValues.Default.SAVE_IMAGE_AS_PREVIEW
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.SAVE_IMAGE_AS_PREVIEW, value)
            editor.apply()
        }

    var saveVideoAsPreviewed: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.SAVE_VIDEO_AS_PREVIEW,
                SettingValues.Default.SAVE_VIDEO_AS_PREVIEW
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(SettingValues.Key.SAVE_VIDEO_AS_PREVIEW, value)
            editor.apply()
        }

    var storageLocation: String
        get() {
            return commonPref.getString(
                SettingValues.Key.STORAGE_LOCATION,
                SettingValues.Default.STORAGE_LOCATION
            )!!
        }
        set(value) {
            val cur = storageLocation
            if (cur != SettingValues.Default.STORAGE_LOCATION) {
                CapturedItems.savePreviousSafTree(Uri.parse(cur), commonPref)
            }

            val editor = commonPref.edit()
            editor.putString(SettingValues.Key.STORAGE_LOCATION, value)
            editor.apply()
        }

    var photoQuality: Int
        get() {
            return commonPref.getInt(
                SettingValues.Key.PHOTO_QUALITY,
                SettingValues.Default.PHOTO_QUALITY
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putInt(SettingValues.Key.PHOTO_QUALITY, value)
            editor.apply()
        }

    var removeExifAfterCapture: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.REMOVE_EXIF_AFTER_CAPTURE,
                SettingValues.Default.REMOVE_EXIF_AFTER_CAPTURE
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(
                SettingValues.Key.REMOVE_EXIF_AFTER_CAPTURE,
                value
            )
            editor.apply()
        }

    var gSuggestions: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.GYROSCOPE_SUGGESTIONS,
                SettingValues.Default.GYROSCOPE_SUGGESTIONS
            )
        }
        set(value) {
            val editor = commonPref.edit()
            editor.putBoolean(
                SettingValues.Key.GYROSCOPE_SUGGESTIONS,
                value
            )
            editor.apply()
        }

    val isZslSupported : Boolean by lazy {
        camera!!.cameraInfo.isZslSupported
    }

    fun isVideoStabilizationSupported() : Boolean {
        return isRecorderStabilizationSupported()
    }

    private fun isPreviewStabilizationSupported() : Boolean {
        return Preview.getPreviewCapabilities(getCurrentCameraInfo()).isStabilizationSupported
    }


    private fun isRecorderStabilizationSupported() : Boolean {
        return Recorder.getVideoCapabilities(getCurrentCameraInfo()).isStabilizationSupported
    }

    fun shouldShowGyroscope(): Boolean {
        return isInPhotoMode && gSuggestions
    }

    private val isInPhotoMode: Boolean
        get() {
            return !(isQRMode || isVideoMode)
        }

    val isInCaptureMode: Boolean
        get() {
            return mActivity is CaptureActivity
        }

//    var imageFormat : String
//        get() {
//            return commonPref.getString(
//                SettingValues.Key.IMAGE_FILE_FORMAT,
//                SettingValues.Default.IMAGE_FILE_FORMAT
//            )!!
//        }
//        set(value) {
//            val editor = commonPref.edit()
//            editor.putString(
//                SettingValues.Key.IMAGE_FILE_FORMAT,
//                value
//            )
//            editor.apply()
//        }
//
//    var videoFormat : String
//        get() {
//            return commonPref.getString(
//                SettingValues.Key.VIDEO_FILE_FORMAT,
//                SettingValues.Default.VIDEO_FILE_FORMAT
//            )!!
//        }
//        set(value) {
//            val editor = commonPref.edit()
//            editor.putString(
//                SettingValues.Key.VIDEO_FILE_FORMAT,
//                value
//            )
//            editor.apply()
//        }

    private fun saveLastCapturedItem(item: CapturedItem, editor: SharedPreferences.Editor) {
        editor.putInt(SettingValues.Key.LAST_CAPTURED_ITEM_TYPE, item.type)
        editor.putString(SettingValues.Key.LAST_CAPTURED_ITEM_DATE_STRING, item.dateString)
        editor.putString(SettingValues.Key.LAST_CAPTURED_ITEM_URI, item.uri.toString())
    }

    fun updateLastCapturedItem(item: CapturedItem) {
        commonPref.edit {
            saveLastCapturedItem(item, this)
        }

        if (mActivity is SecureMainActivity) {
            // previous call updated ephemeral SharedPreferences that won't be accessible by the
            // "regular" MainActivity
            mActivity.applicationContext.getSharedPreferences(COMMON_SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit {
                saveLastCapturedItem(item, this)
            }
        }

        lastCapturedItem = item
    }

    var requireLocation: Boolean = false
        get() {
            return mActivity.settingsDialog.locToggle.isChecked
        }
        set(value) {
            mActivity.locationCamConfigChanged(value)
            modePref.edit {
                putBoolean(SettingValues.Key.GEO_TAGGING, value)
            }

            mActivity.settingsDialog.locToggle.isChecked = value

            field = value
        }

    var selfIlluminate: Boolean
        get() {
            return modePref.getBoolean(
                SettingValues.Key.SELF_ILLUMINATION,
                SettingValues.Default.SELF_ILLUMINATION
            )
                    && lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        set(value) {
            modePref.edit {
                putBoolean(SettingValues.Key.SELF_ILLUMINATION, value)
            }

            mActivity.settingsDialog.selfIlluminationToggle.isChecked = value
            mActivity.settingsDialog.selfIllumination()
        }

    private fun getString(@StringRes id: Int) = mActivity.getString(id)

    fun setQRScanningFor(format: String, selected: Boolean) {

        val formatSRep = "${SettingValues.Key.SCAN}_$format"

        commonPref.edit {
            putBoolean(formatSRep, selected)
        }

        if (selected) {
            if (BarcodeFormat.valueOf(format) !in allowedFormats) {
                allowedFormats.add(BarcodeFormat.valueOf(format))
            }
        } else {
            if (allowedFormats.size == 1) {
                mActivity.showMessage(
                    getString(R.string.no_barcode_selected)
                )
            } else {
                allowedFormats.remove(BarcodeFormat.valueOf(format))
            }
        }

        qrAnalyzer?.refreshHints()
    }

    fun reloadSettings() {
        // pref config needs to be created
        modePref.edit {
            if (!modePref.contains(SettingValues.Key.FLASH_MODE)) {
                putInt(SettingValues.Key.FLASH_MODE, SettingValues.Default.FLASH_MODE)
            }

            if (!modePref.contains(SettingValues.Key.GEO_TAGGING)) {
                putBoolean(SettingValues.Key.GEO_TAGGING, SettingValues.Default.GEO_TAGGING)
            }

            if (isVideoMode) {
                mActivity.settingsDialog.reloadVideoSettings()
            }

            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                if (!modePref.contains(SettingValues.Key.SELF_ILLUMINATION)) {
                    putBoolean(
                        SettingValues.Key.SELF_ILLUMINATION,
                        SettingValues.Default.SELF_ILLUMINATION
                    )
                }
            }
        }

        flashMode = modePref.getInt(
            SettingValues.Key.FLASH_MODE,
            SettingValues.Default.FLASH_MODE
        )

        requireLocation = modePref.getBoolean(
            SettingValues.Key.GEO_TAGGING,
            SettingValues.Default.GEO_TAGGING
        )

        selfIlluminate = modePref.getBoolean(
            SettingValues.Key.SELF_ILLUMINATION,
            SettingValues.Default.SELF_ILLUMINATION
        )

        mActivity.settingsDialog.showOnlyRelevantSettings()
    }

    fun loadSettings() {

        // Create common config. if it's not created
        val editor = commonPref.edit()

        if (!commonPref.contains(SettingValues.Key.CAMERA_SOUNDS)) {
            editor.putBoolean(SettingValues.Key.CAMERA_SOUNDS, SettingValues.Default.CAMERA_SOUNDS)
        }

        // Note: This is a workaround to keep save image/video as previewed 'on' by
        // default starting from v73 and 'off' by default for versions before that
        //
        // If its not a fresh install (before v73)
        if (commonPref.contains(SettingValues.Key.SAVE_IMAGE_AS_PREVIEW)) {
            // If save video as previewed was not previously set
            if (!commonPref.contains(SettingValues.Key.SAVE_VIDEO_AS_PREVIEW)) {
                // Explicitly set the value for this setting as false for them
                // to ensure consistent behavior
                editor.putBoolean(
                    SettingValues.Key.SAVE_VIDEO_AS_PREVIEW,
                    false
                )
            }
        } else {
            editor.putBoolean(
                SettingValues.Key.SAVE_IMAGE_AS_PREVIEW,
                SettingValues.Default.SAVE_IMAGE_AS_PREVIEW
            )

            editor.putBoolean(
                SettingValues.Key.SAVE_VIDEO_AS_PREVIEW,
                SettingValues.Default.SAVE_VIDEO_AS_PREVIEW
            )
        }

        if (!commonPref.contains(SettingValues.Key.GRID)) {
            // Index for Grid.values() Default: NONE
            editor.putInt(SettingValues.Key.GRID, SettingValues.Default.GRID_TYPE_INDEX)
        }

        if (!commonPref.contains(SettingValues.Key.FOCUS_TIMEOUT)) {
            editor.putString(SettingValues.Key.FOCUS_TIMEOUT, SettingValues.Default.FOCUS_TIMEOUT)
        }

        if (!commonPref.contains(SettingValues.Key.EMPHASIS_ON_QUALITY)) {
            editor.putBoolean(
                SettingValues.Key.EMPHASIS_ON_QUALITY,
                SettingValues.Default.EMPHASIS_ON_QUALITY
            )
        }

        if (!commonPref.contains(SettingValues.Key.INCLUDE_AUDIO)) {
            editor.putBoolean(
                SettingValues.Key.INCLUDE_AUDIO,
                SettingValues.Default.INCLUDE_AUDIO
            )
        }

        if (!commonPref.contains(SettingValues.Key.ENABLE_EIS)) {
            editor.putBoolean(
                SettingValues.Key.ENABLE_EIS,
                SettingValues.Default.ENABLE_EIS
            )
        }

        if (!commonPref.contains(SettingValues.Key.ASPECT_RATIO)) {
            editor.putInt(
                SettingValues.Key.ASPECT_RATIO,
                SettingValues.Default.ASPECT_RATIO
            )
        }

        if (!commonPref.contains(SettingValues.Key.SCAN_ALL_CODES)) {
            editor.putBoolean(
                SettingValues.Key.SCAN_ALL_CODES,
                SettingValues.Default.SCAN_ALL_CODES
            )
        }

        val qrRep = "${SettingValues.Key.SCAN}_${BarcodeFormat.QR_CODE.name}"

        if (!commonPref.contains(qrRep)) {
            for (format in BarcodeFormat.values()) {
                val formatSRep = "${SettingValues.Key.SCAN}_${format.name}"

                editor.putBoolean(
                    formatSRep,
                    false
                )
            }

            editor.putBoolean(
                qrRep,
                true
            )
        }


        editor.apply()

        gridType = GridType.values()[commonPref.getInt(
            SettingValues.Key.GRID,
            SettingValues.Default.GRID_TYPE_INDEX
        )]

        mActivity.settingsDialog.updateGridToggleUI()

        commonPref.getString(SettingValues.Key.FOCUS_TIMEOUT, SettingValues.Default.FOCUS_TIMEOUT)
            ?.let {
                mActivity.settingsDialog.updateFocusTimeout(it)
            }

        if (emphasisQuality) {
            mActivity.settingsDialog.cmRadioGroup.check(R.id.quality_radio)
        } else {
            mActivity.settingsDialog.cmRadioGroup.check(R.id.latency_radio)
        }

        aspectRatio = commonPref.getInt(
            SettingValues.Key.ASPECT_RATIO,
            SettingValues.Default.ASPECT_RATIO
        )

        includeAudio = commonPref.getBoolean(
            SettingValues.Key.INCLUDE_AUDIO,
            SettingValues.Default.INCLUDE_AUDIO
        )

        enableEIS = commonPref.getBoolean(
            SettingValues.Key.ENABLE_EIS,
            SettingValues.Default.ENABLE_EIS
        )

        allowedFormats.clear()

        for (format in BarcodeFormat.values()) {
            val formatSRep = "${SettingValues.Key.SCAN}_${format.name}"

            val isEnabled = commonPref.getBoolean(
                formatSRep,
                false
            )

            if (isEnabled) {
                if (format !in allowedFormats) {
                    allowedFormats.add(format)
                }

                if (format == BarcodeFormat.QR_CODE) {
                    mActivity.qrToggle.isSelected = true
                }

                if (format == BarcodeFormat.AZTEC) {
                    mActivity.azToggle.isSelected = true
                }

                if (format == BarcodeFormat.PDF_417) {
                    mActivity.cBToggle.isSelected = true
                }

                if (format == BarcodeFormat.DATA_MATRIX) {
                    mActivity.dmToggle.isSelected = true
                }
            }
        }

        qrAnalyzer?.refreshHints()
    }

    var emphasisQuality: Boolean
        get() {
            return commonPref.getBoolean(
                SettingValues.Key.EMPHASIS_ON_QUALITY,
                SettingValues.Default.EMPHASIS_ON_QUALITY
            )
        }
        set(value) {
            commonPref.edit {
                putBoolean(SettingValues.Key.EMPHASIS_ON_QUALITY, value)
            }
        }

    fun toggleTorchState() {
        isTorchOn = !isTorchOn
    }

    fun toggleFlashMode() {
        if (isFlashAvailable) {

            flashMode = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }

        } else {
            mActivity.showMessage(
                getString(R.string.flash_unavailable_in_selected_mode)
            )
        }
    }

    fun toggleAspectRatio() {
        aspectRatio = if (aspectRatio == AspectRatio.RATIO_16_9) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
        startCamera(true)
    }

    private fun getCurrentCameraInfo() : CameraInfo {
        return cameraProvider!!.getCameraInfo(cameraSelector)
    }

    fun getAvailableVideoFrameRates():  List<Range<Int>>  {
        val resSet = getCurrentCameraInfo().supportedFrameRateRanges

        // Individual fps -> Ranged fps (sorted by lower value of range and then upper for each lower value)
        val resList = resSet.sortedWith(compareBy<Range<Int>> { it.lower != it.upper }.thenBy { it.lower }.thenBy { it.upper })

        // If the supportedFrameRateRange list is somehow empty due to device/library implementation
        // go with the most likely default rate
        if (resList.isEmpty()) return listOf(SettingValues.Default.VIDEO_FRAME_RATE)

        return resList
    }

    fun toggleCameraSelector() {

        // Manually switch to the opposite lens facing
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK


        // Test whether the new lens facing is supported by the current device
        // If it is supported then restart the camera with the new configuration
        if (isLensFacingSupported(lensFacing)) {
            startCamera(true)
        } else {
            // Else revert back to the old facing (while displaying an error message
            // to the user)
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                mActivity.showMessage(getString(R.string.rear_camera_unavailable))
                CameraSelector.LENS_FACING_FRONT
            } else {
                mActivity.showMessage(getString(R.string.front_camera_unavailable))
                CameraSelector.LENS_FACING_BACK
            }
        }

    }

    fun initializeCamera(forced: Boolean = false) {
        if (cameraProvider != null) {
            startCamera(forced = forced)
            return
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mActivity)

        cameraProviderFuture.addListener(fun() {
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: ExecutionException) {
                mActivity.showMessage(mActivity.getString(R.string.camera_provider_init_failure))
                return
            }

            // Manually switch to the other lens facing (if the default lens facing isn't
            // supported for the current device)
            if (!isLensFacingSupported(lensFacing)) {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }

            val extensionsManagerFuture =
                ExtensionsManager.getInstanceAsync(mActivity, cameraProvider!!)

            extensionsManagerFuture.addListener({
                try {
                    extensionsManager = extensionsManagerFuture.get()
                } catch (e: ExecutionException) {
                    mActivity.showMessage(mActivity.getString(R.string.extensions_manager_init_failure))
                }
                startCamera(forced = forced)
            }, ContextCompat.getMainExecutor(mActivity))

        }, ContextCompat.getMainExecutor(mActivity))
    }

    private fun isLensFacingSupported(lensFacing : Int) : Boolean {
        var tCameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        if (currentMode.extensionMode != ExtensionMode.NONE) {
            extensionsManager?.let { em ->
                if (!em.isExtensionAvailable(tCameraSelector, currentMode.extensionMode))
                    return false

                try {
                    tCameraSelector = em.getExtensionEnabledCameraSelector(tCameraSelector, currentMode.extensionMode)
                } catch (e : IllegalArgumentException) {
                    return false
                }
            }
        }

        return cameraProvider?.hasCamera(tCameraSelector) ?: false
    }

    // Start the camera with latest hard configuration
    fun startCamera(forced: Boolean = false) {
        if ((!forced && camera != null) || cameraProvider == null) return

        // Cancel any pending capture requests
        mActivity.imageCapturer.cancelPendingCaptureRequest()

        mActivity.exposureBar.hidePanel()
        modePref = mActivity.getSharedPreferences(currentMode.name, Context.MODE_PRIVATE)

        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = mActivity.display
            display?.rotation ?: @Suppress("DEPRECATION")
            mActivity.windowManager.defaultDisplay.rotation
        } else {
            // We don't really have any option here, but this initialization
            // ensures that the app doesn't break later when the below
            // deprecated option gets removed post Android R
            @Suppress("DEPRECATION")
            mActivity.windowManager.defaultDisplay.rotation
        }

        if (mActivity.isDestroyed || mActivity.isFinishing) return

        // Test whether the current lens facing is supported by the current device
        // If not then silently switch to the other lens facing
        // (Snackbar/popup message can be shown before startCamera is called
        // in specific cases of explicitly switching to another side or if
        // the camera is expected)
        if (!isLensFacingSupported(lensFacing)) {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        }


        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val builder = ImageCapture.Builder()

        // To use the last frame instead of showing a blank screen when
        // the camera that is being currently used gets unbind
        mActivity.updateLastFrame()

        // Unbind/close all other camera(s) [if any]
        cameraProvider?.unbindAll()

        val extMode = currentMode.extensionMode
        if (extMode != ExtensionMode.NONE) {
            val em = extensionsManager
            if (em != null && em.isExtensionAvailable(cameraSelector, extMode)) {
                cameraSelector = em.getExtensionEnabledCameraSelector(cameraSelector, extMode)
            } else {
                Log.e(TAG, "Mode $currentMode isn't available for this device")
            }
        }

        val useCaseGroupBuilder = UseCaseGroup.Builder()
        val aspectRatioStrategy = AspectRatioStrategy(
            aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO
        )

        if (isQRMode) {
            val analyzer = QRAnalyzer(mActivity)
            val strategy = ResolutionStrategy(Size(960, 960),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            val mIAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder().setResolutionStrategy(strategy).build())
                .setOutputImageRotationEnabled(true)
                .build()
            qrAnalyzer = analyzer
            mActivity.startFocusTimer()
            iAnalyzer = mIAnalyzer
            mIAnalyzer.setAnalyzer(cameraExecutor, analyzer)
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(
                    if (isLensFacingSupported(CameraSelector.LENS_FACING_BACK)) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        mActivity.showMessage(R.string.qr_rear_camera_unavailable)
                        CameraSelector.LENS_FACING_FRONT
                    })
                .build()
            useCaseGroupBuilder.addUseCase(mIAnalyzer)

        } else {
            if (isVideoMode) {

                mActivity.micOffIcon.visibility =
                    if (includeAudio) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                val videoCaptureBuilder = VideoCapture.Builder(
                    Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(videoQuality))
                    .build()
                )

                videoCaptureBuilder.setVideoStabilizationEnabled(mActivity.camConfig.enableEIS)

                if (mActivity.camConfig.saveVideoAsPreviewed)
                    videoCaptureBuilder.setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)

                videoCaptureBuilder.setTargetFrameRate(mActivity.camConfig.videoFrameRate)

                videoCapture = videoCaptureBuilder.build()

                useCaseGroupBuilder.addUseCase(videoCapture!!)
            }

            if (!mActivity.requiresVideoModeOnly) {
                imageCapture = builder.let {
                    it.setCaptureMode(
                        if (emphasisQuality) {
                            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                        } else {
                            if (enableZsl) {
                                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                            } else {
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                            }
                        }
                    )

                    it.setTargetRotation(
                        imageCapture?.targetRotation
                            ?: rotation
                    )

                    it.setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(aspectRatioStrategy)
                            .build()
                    )

                    it.setFlashMode(flashMode)

                    if (photoQuality != SettingValues.Default.PHOTO_QUALITY) {
                        it.setJpegQuality(photoQuality)
                    }

                    it.build()
                }

                useCaseGroupBuilder.addUseCase(imageCapture!!)
            }
        }

        val previewBuilder = Preview.Builder()
            .setTargetRotation(
                preview?.targetRotation
                    ?: rotation
            )
            .setResolutionSelector(
                ResolutionSelector.Builder().setAspectRatioStrategy(aspectRatioStrategy).build()
            )

        if (isVideoMode && isPreviewStabilizationSupported()) {
            previewBuilder.setPreviewStabilizationEnabled(enableEIS)
        } else {
            previewBuilder.setPreviewStabilizationEnabled(false)
        }

        preview = previewBuilder.build().also {
            useCaseGroupBuilder.addUseCase(it)
            it.setSurfaceProvider(mActivity.previewView.surfaceProvider)
        }

        mActivity.forceUpdateOrientationSensor()

        try {
            try {
                camera = cameraProvider!!.bindToLifecycle(
                    mActivity, cameraSelector,
                    useCaseGroupBuilder.build()
                )
            } catch (exception: IllegalArgumentException) {
                if (isVideoMode) {
                    val newUseCaseGroupBuilder = UseCaseGroup.Builder()
                    videoCapture?.let {
                        newUseCaseGroupBuilder.addUseCase(it)
                    }
                    preview?.let {
                        newUseCaseGroupBuilder.addUseCase(it)
                    }
                    imageCapture = null

                    camera = cameraProvider!!.bindToLifecycle(
                        mActivity, cameraSelector,
                        newUseCaseGroupBuilder.build()
                    )
                } else {
                    throw exception
                }
            }
        } catch (exception: IllegalArgumentException) {
            mActivity.showMessage(mActivity.getString(R.string.bind_failure))
            return
        }

        loadTabs()

        camera?.cameraInfo?.zoomState?.observe(mActivity) {
            if (it.linearZoom != 0f || it.zoomRatio != 1f) {
                mActivity.zoomBar.updateThumb()
            }
        }

        mActivity.zoomBar.updateThumb(false)

        camera?.cameraInfo?.exposureState?.let { mActivity.exposureBar.setExposureConfig(it) }

        mActivity.settingsDialog.torchToggle.isChecked = false

        // Focus camera on touch/tap
        mActivity.previewView.setOnTouchListener(mActivity)
        mActivity.previewView.apply {
            when (aspectRatio) {
                AspectRatio.RATIO_16_9 -> {
                    markAs16by9Layout()
                }
                AspectRatio.RATIO_4_3 -> {
                    markAs4by3Layout()
                }
            }
        }

        if (isInPhotoMode) {
            mActivity.sensorNotifier?.forceUpdateGyro()
        } else {
            mActivity.gCircleFrame.visibility = View.GONE
        }
    }

    fun snapPreview() {

        if (selfIlluminate) {

            val animation: Animation = AlphaAnimation(0f, 0.8f)
            animation.duration = PREVIEW_SL_OVERLAY_DUR
            animation.interpolator = LinearInterpolator()
            animation.fillAfter = true

            mActivity.mainOverlay.setImageResource(android.R.color.white)

            animation.setAnimationListener(
                object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {
                        mActivity.mainOverlay.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(p0: Animation?) {}

                    override fun onAnimationRepeat(p0: Animation?) {}

                }
            )

            mActivity.mainOverlay.startAnimation(animation)

        } else {

            val animation: Animation = AlphaAnimation(1f, 0f)
            animation.duration = PREVIEW_SNAP_DURATION
            animation.interpolator = LinearInterpolator()
            animation.repeatMode = Animation.REVERSE

            mActivity.mainOverlay.setImageResource(android.R.color.black)

            animation.setAnimationListener(
                object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {
                        mActivity.mainOverlay.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        mActivity.mainOverlay.visibility = View.INVISIBLE
                        mActivity.mainOverlay.setImageResource(android.R.color.transparent)
                    }

                    override fun onAnimationRepeat(p0: Animation?) {}

                }
            )

            mActivity.mainOverlay.startAnimation(animation)
        }
    }

    private fun availableModes(): Set<CameraMode> {
        return CameraMode.entries.filter {
            when (it) {
                CameraMode.CAMERA, CameraMode.VIDEO -> true
                CameraMode.QR_SCAN -> mActivity !is SecureMainActivity
                else -> {
                    check(it.extensionMode != ExtensionMode.NONE)
                    val em = extensionsManager
                    if (em != null) {
                        em.isExtensionAvailable(FRONT_CAMERA_SELECTOR, it.extensionMode) || em.isExtensionAvailable(REAR_CAMERA_SELECTOR, it.extensionMode)
                    } else {
                        false
                    }
                }
            }
        }.toSet()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadTabs() {
        if (!mActivity.shouldShowCameraModeTabs()) {
            return
        }

        val tabLayout = mActivity.tabLayout
        val availableModes = availableModes()

        if (availableModes == tabLayout.getAllModes()) {
            return
        }

        Log.i(TAG, "Refreshing tabs...")

        tabLayout.removeAllTabs()

        availableModes.forEach { mode ->
            tabLayout.newTab().let { tab ->
                tab.setText(mode.uiName)

                tab.view.setOnTouchListener { _, e ->
                    if (e.action == MotionEvent.ACTION_UP) {
                        mActivity.finalizeMode(tab)
                    }
                    false
                }
                tab.tag = mode

                tabLayout.addTab(tab, mode == DEFAULT_CAMERA_MODE)
            }
        }
    }

    fun switchMode(mode: CameraMode) {
        if (currentMode == mode) {
            return
        }

        currentMode = mode

        mActivity.cancelFocusTimer()

        isQRMode = mode == CameraMode.QR_SCAN

        isVideoMode = mode == CameraMode.VIDEO

        if (isQRMode) {
            mActivity.qrOverlay.visibility = View.VISIBLE
            mActivity.thirdOption.visibility = View.INVISIBLE

            if (scanAllCodes) {
                mActivity.flipCamIcon.setImageResource(
                    R.drawable.cancel
                )
                mActivity.qrScanToggles.visibility = View.GONE
            } else {
                mActivity.flipCamIcon.setImageResource(
                    R.drawable.auto
                )
                mActivity.qrScanToggles.visibility = View.VISIBLE
            }

            mActivity.cancelButtonView.visibility = View.INVISIBLE

            mActivity.captureButton.setBackgroundResource(android.R.color.transparent)
            mActivity.captureButton.setImageResource(R.drawable.torch_off_button)

            mActivity.micOffIcon.visibility = View.GONE
        } else {
            mActivity.qrOverlay.visibility = View.INVISIBLE
            mActivity.thirdOption.visibility = View.VISIBLE
            mActivity.flipCamIcon.setImageResource(R.drawable.flip_camera)
            mActivity.cancelButtonView.visibility = View.VISIBLE

            mActivity.qrScanToggles.visibility = View.GONE

            mActivity.captureButton.setBackgroundResource(R.drawable.cbutton_bg)

            if (isVideoMode) {
                mActivity.captureButton.setImageResource(R.drawable.recording)
            } else {
                mActivity.captureButton.setImageResource(R.drawable.camera_shutter)
                mActivity.micOffIcon.visibility = View.GONE
            }
        }

        mActivity.cbText.visibility = if (isQRMode || isVideoMode || mActivity.timerDuration == 0) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        startCamera(true)
    }

    fun showMoreOptionsForQR() {
        val builder = MaterialAlertDialogBuilder(mActivity)
        builder.setTitle(mActivity.resources.getString(R.string.more_options))

        val optionNames = arrayListOf<String>()
        val optionValues = arrayListOf<Boolean>()

        for (format in BarcodeFormat.values()) {

            if (format in commonFormats) continue

            optionNames.add(format.name)

            val formatSRep = "${SettingValues.Key.SCAN}_$format"
            optionValues.add(
                commonPref.getBoolean(
                    formatSRep,
                    false
                )
            )
        }

        builder.setMultiChoiceItems(
            optionNames.toArray(arrayOf<String>()),
            optionValues.toBooleanArray()
        ) { _, index, isChecked ->
            optionValues[index] = isChecked
        }

        // Add OK and Cancel buttons
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->

            val allCommonFormatsDisabled = commonFormats.none {
                allowedFormats.contains(it)
            }

            // If all formats displayed outside the dialog are disabled (main QR scanner
            // UI)
            if (allCommonFormatsDisabled) {
                val noOptionWasChecked = optionValues.none { it }

                // If no option is selected within the check box too (implying no barcode format
                // is selected at all) - don't make apply the selction made by the user
                if (noOptionWasChecked) {
                    mActivity.showMessage(
                        getString(R.string.no_barcode_selected)
                    )
                    return@setPositiveButton
                }
            }

            commonPref.edit {
                for (index in 0 until optionNames.size) {

                    val optionName = optionNames[index]
                    val optionValue = optionValues[index]

                    val formatSRep = "${SettingValues.Key.SCAN}_$optionName"

                    val format = BarcodeFormat.valueOf(optionName)

                    if (optionValue) {
                        if (format !in allowedFormats)
                            allowedFormats.add(format)
                    } else {
                        allowedFormats.remove(format)
                    }

                    putBoolean(formatSRep, optionValue)
                }
            }

            qrAnalyzer?.refreshHints()
        }

        builder.setNegativeButton(R.string.cancel, null)

        // Create and show the alert dialog
        val dialog = builder.create()

        dialog.setOnShowListener {
            val button: Button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener {

            }
        }

        dialog.showIgnoringShortEdgeMode()
    }

    fun onStorageLocationNotFound() {
        // Reverting back to DEFAULT_MEDIA_STORE_CAPTURE_PATH
        storageLocation = SettingValues.Default.STORAGE_LOCATION

        val builder = MaterialAlertDialogBuilder(mActivity)
            .setTitle(R.string.folder_not_found)
            .setMessage(R.string.reverting_to_default_folder)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.more_settings) { _, _ ->
                MoreSettings.start(mActivity)
            }
        val alertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.showIgnoringShortEdgeMode()
    }
}
