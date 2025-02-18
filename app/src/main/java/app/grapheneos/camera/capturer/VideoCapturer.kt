package app.grapheneos.camera.capturer

import android.Manifest
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore.MediaColumns
import android.view.View
import android.webkit.MimeTypeMap
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import app.grapheneos.camera.App
import app.grapheneos.camera.CamConfig
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.R
import app.grapheneos.camera.VIDEO_NAME_PREFIX
import app.grapheneos.camera.ui.activities.MainActivity
import app.grapheneos.camera.ui.activities.SecureMainActivity
import app.grapheneos.camera.ui.activities.VideoCaptureActivity
import app.grapheneos.camera.util.getTreeDocumentUri
import app.grapheneos.camera.util.removePendingFlagFromUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoCapturer(private val mActivity: MainActivity) {

    val camConfig = mActivity.camConfig

    var isRecording = false
        private set

    private val videoFileFormat = ".mp4"

    private var recording: Recording? = null

    var isMuted = false
        private set

    var includeAudio: Boolean = false

    var isPaused = false
        set(value) {
            if (isRecording) {
                if (value) {
                    recording?.pause()
                    mActivity.flipCamIcon.setImageResource(R.drawable.play)
                } else {
                    recording?.resume()
                    mActivity.flipCamIcon.setImageResource(R.drawable.pause)
                }
            }
            field = value
        }

    private val handler = Handler(Looper.getMainLooper())

    private fun updateTimerTime(timeInNanos: Long) {
        val timeInSec = timeInNanos / (1000 * 1000 * 1000)
        val sec = timeInSec % 60
        val min = timeInSec / 60 % 60
        val hour = timeInSec / 3600

        val timerText: String = if (hour == 0L) {
            String.format(Locale.ROOT, "%02d:%02d", min, sec)
        } else {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hour, min, sec)
        }

        mActivity.timerView.text = timerText
    }

    private class RecordingContext(
        val pendingRecording: PendingRecording,
        val uri: Uri,
        val fileDescriptor: ParcelFileDescriptor,
        val shouldAddToGallery: Boolean,
        val isPendingMediaStoreUri: Boolean,
    )

    private fun createRecordingContext(recorder: Recorder, fileName: String): RecordingContext? {
        val mimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(videoFileFormat) ?: "video/mp4"

        val ctx = mActivity
        val contentResolver = ctx.contentResolver

        val uri: Uri?
        var shouldAddToGallery = true
        var isPendingMediaStoreUri = false

        if (ctx is VideoCaptureActivity && ctx.isOutputUriAvailable()) {
            uri = ctx.outputUri
            shouldAddToGallery = false
        } else {
            val storageLocation = camConfig.storageLocation

            if (storageLocation == CamConfig.SettingValues.Default.STORAGE_LOCATION) {
                val contentValues = ContentValues().apply {
                    put(MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaColumns.MIME_TYPE, mimeType)
                    put(MediaColumns.RELATIVE_PATH, DEFAULT_MEDIA_STORE_CAPTURE_PATH)
                    put(MediaColumns.IS_PENDING, 1)
                }
                uri = contentResolver.insert(CamConfig.videoCollectionUri, contentValues)
                isPendingMediaStoreUri = true
            } else {
                val treeUri = Uri.parse(storageLocation)
                val treeDocumentUri = getTreeDocumentUri(treeUri)

                uri = DocumentsContract.createDocument(contentResolver, treeDocumentUri, mimeType, fileName)
            }
        }

        if (uri == null) {
            return null
        }

        var location: Location? = null
        if (camConfig.requireLocation) {
            location = (mActivity.applicationContext as App).getLocation()
            if (location == null) {
                mActivity.showMessage(R.string.location_unavailable)
            }
        }
        contentResolver.openFileDescriptor(uri,"w")?.let {
            val outputOptions = FileDescriptorOutputOptions.Builder(it)
                .setLocation(location)
                .build()
            val pendingRecording = recorder.prepareRecording(ctx, outputOptions)
            return RecordingContext(pendingRecording, uri, it, shouldAddToGallery, isPendingMediaStoreUri)
        }
        return null
    }

    fun startRecording() {
        if (camConfig.camera == null) return
        val recorder = camConfig.videoCapture?.output ?: return
        if (isRecording) return
        isRecording = true

        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = VIDEO_NAME_PREFIX + dateString + videoFileFormat

        includeAudio = false

        val ctx = mActivity

        if (ctx.settingsDialog.includeAudioToggle.isChecked) {
            if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED) {
                includeAudio = true
            } else {
                ctx.restartRecordingWithMicPermission()
                isRecording = false
                return
            }
        }

        val recordingCtx = try {
            createRecordingContext(recorder, fileName)!!
        } catch (exception: Exception) {
            val foreignUri = ctx is VideoCaptureActivity && ctx.isOutputUriAvailable()
            if (!foreignUri) {
                camConfig.onStorageLocationNotFound()
            }
            ctx.showMessage(R.string.unable_to_access_output_file)
            isRecording = false
            return
        }

        val pendingRecording = recordingCtx.pendingRecording

        if (includeAudio) {
            pendingRecording.withAudioEnabled()
        }

        beforeRecordingStarts()

        camConfig.mPlayer.playVRStartSound(handler) {

            recording = pendingRecording.start(ctx.mainExecutor) { event ->

                if (event is VideoRecordEvent.Start) {
                    onRecordingStart()
                }

                if (event is VideoRecordEvent.Status) {
                    updateTimerTime(event.recordingStats.recordedDurationNanos)
                }

                if (event is VideoRecordEvent.Finalize) {
                    afterRecordingStops()

                    camConfig.mPlayer.playVRStopSound()

                    if (event.hasError()) {
                        when (event.error) {
                            VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> {
                                ctx.showMessage(R.string.recording_too_short_to_be_saved)
                                return@start
                            }
                            VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED,
                            VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR,
                            VideoRecordEvent.Finalize.ERROR_UNKNOWN -> {
                                ctx.showMessage(ctx.getString(R.string.unable_to_save_video_verbose, event.error))
                                return@start
                            }
                            else -> {
                                ctx.showMessage(ctx.getString(R.string.error_during_recording, event.error))
                            }
                        }
                    }

                    val uri = recordingCtx.uri

                    if (recordingCtx.isPendingMediaStoreUri) {
                        try {
                            removePendingFlagFromUri(ctx.contentResolver, uri)
                        } catch (e: Exception) {
                            ctx.showMessage(R.string.unable_to_save_video)
                        }
                    }

                    if (recordingCtx.shouldAddToGallery) {
                        val item = CapturedItem(ITEM_TYPE_VIDEO, dateString, uri)
                        camConfig.updateLastCapturedItem(item)

                        ctx.updateThumbnail()

                        if (ctx is SecureMainActivity) {
                            ctx.capturedItems.add(item)
                        }
                    }

                    if (ctx is VideoCaptureActivity) {
                        ctx.afterRecording(uri)
                    }
                }
            }

            try {
                // FileDescriptorOutputOptions doc says that the file descriptor should be closed by the
                // caller, and that it's safe to do so as soon as pendingRecording.start() returns
                recordingCtx.fileDescriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val dp16 = 16 * mActivity.resources.displayMetrics.density
    private val dp8 = 8 * mActivity.resources.displayMetrics.density

    private fun beforeRecordingStarts() {
        mActivity.previewView.keepScreenOn = true
    }

    private fun onRecordingStart() {
        // TODO: Uncomment this once the main indicator UI gets implemented
        // mActivity.micOffIcon.visibility = View.GONE

        val gd: GradientDrawable = mActivity.captureButton.drawable as GradientDrawable

        val animator = ValueAnimator.ofFloat(dp16, dp8)

        animator.setDuration(300)
            .addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                gd.cornerRadius = value
            }

        animator.start()

        mActivity.settingsDialog.videoQualitySpinner.isEnabled = false
        mActivity.settingsDialog.videoFrameRateSpinner.isEnabled = false
        mActivity.settingsDialog.enableEISToggle.isEnabled = false

        mActivity.flipCamIcon.setImageResource(R.drawable.pause)
        isPaused = false
        mActivity.cancelButtonView.visibility = View.GONE

        if (mActivity.requiresVideoModeOnly) {
            mActivity.thirdOption.visibility = View.INVISIBLE
        }

        mActivity.settingsDialog.lRadio.isEnabled = false
        mActivity.settingsDialog.qRadio.isEnabled = false

        mActivity.thirdCircle.setImageResource(R.drawable.camera_shutter)
        mActivity.tabLayout.visibility = View.INVISIBLE
        mActivity.timerView.setText(R.string.start_value_timer)
        mActivity.timerView.visibility = View.VISIBLE

        mActivity.settingsDialog.includeAudioToggle.isEnabled = false

        if (camConfig.includeAudio) {
            isMuted = false
            mActivity.muteToggle.setImageResource(R.drawable.mic_on)
            mActivity.muteToggle.setBackgroundColor(mActivity.getColor(R.color.red))
            mActivity.muteToggle.tooltipText = mActivity.getString(R.string.tap_to_mute_audio)
            mActivity.muteToggle.visibility = View.VISIBLE
        }
    }

    private fun afterRecordingStops() {

        val gd: GradientDrawable = mActivity.captureButton.drawable as GradientDrawable

        val animator = ValueAnimator.ofFloat(dp8, dp16)

        animator.setDuration(300)
            .addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                gd.cornerRadius = value
            }

        animator.start()

        mActivity.timerView.visibility = View.GONE
        mActivity.flipCamIcon.setImageResource(R.drawable.flip_camera)

        mActivity.settingsDialog.videoQualitySpinner.isEnabled = true
        mActivity.settingsDialog.videoFrameRateSpinner.isEnabled = true
        mActivity.settingsDialog.enableEISToggle.isEnabled = true

        if (mActivity !is VideoCaptureActivity) {
            mActivity.thirdOption.visibility = View.VISIBLE
        }

        if (!mActivity.requiresVideoModeOnly) {
            mActivity.settingsDialog.lRadio.isEnabled = true
            mActivity.settingsDialog.qRadio.isEnabled = true
        }

        if (mActivity !is VideoCaptureActivity) {
            mActivity.thirdCircle.setImageResource(R.drawable.option_circle)
            mActivity.cancelButtonView.visibility = View.VISIBLE
            mActivity.tabLayout.visibility = View.VISIBLE
        }

        mActivity.previewView.keepScreenOn = false

        // TODO: Uncomment this once the main indicator UI gets implemented
        // if (!mActivity.config.includeAudio)
        //   mActivity.micOffIcon.visibility = View.VISIBLE

        mActivity.settingsDialog.includeAudioToggle.isEnabled = true
        mActivity.muteToggle.visibility = View.GONE

        isRecording = false

        mActivity.forceUpdateOrientationSensor()
    }

    fun muteRecording() {
        check(isRecording && camConfig.includeAudio)
        isMuted = true
        recording?.mute(true)
    }

    fun unmuteRecording() {
        check(isRecording && camConfig.includeAudio)
        isMuted = false
        recording?.mute(false)
    }

    fun stopRecording() {
        recording?.stop()
        recording?.close()
        recording = null
    }
}

@Throws(Exception::class)
fun getVideoThumbnail(context: Context, uri: Uri?): Bitmap? {
    MediaMetadataRetriever().use {
        it.setDataSource(context, uri)
        return it.frameAtTime
    }
}
