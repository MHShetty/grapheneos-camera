package app.grapheneos.camera.ui.activities

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.util.Linkify
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.Surface
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.StreamState
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import app.grapheneos.camera.App
import app.grapheneos.camera.CamConfig
import app.grapheneos.camera.CameraMode
import app.grapheneos.camera.ITEM_TYPE_IMAGE
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.R
import app.grapheneos.camera.capturer.ImageCapturer
import app.grapheneos.camera.capturer.VideoCapturer
import app.grapheneos.camera.capturer.getVideoThumbnail
import app.grapheneos.camera.databinding.ActivityMainBinding
import app.grapheneos.camera.databinding.ScanResultDialogBinding
import app.grapheneos.camera.ktx.SystemSettingsObserver
import app.grapheneos.camera.notifier.SensorOrientationChangeNotifier
import app.grapheneos.camera.ui.BottomTabLayout
import app.grapheneos.camera.ui.CountDownTimerUI
import app.grapheneos.camera.ui.CustomGrid
import app.grapheneos.camera.ui.QROverlay
import app.grapheneos.camera.ui.QRToggle
import app.grapheneos.camera.ui.SettingsDialog
import app.grapheneos.camera.ui.seekbar.ExposureBar
import app.grapheneos.camera.ui.seekbar.ZoomBar
import app.grapheneos.camera.ui.showIgnoringShortEdgeMode
import app.grapheneos.camera.util.CameraControl
import app.grapheneos.camera.util.ImageResizer
import app.grapheneos.camera.util.executeIfAlive
import app.grapheneos.camera.util.resolveActivity
import app.grapheneos.camera.util.setBlurBitmapCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.zxing.BarcodeFormat
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

open class MainActivity : AppCompatActivity(),
    OnTouchListener,
    OnScaleGestureListener,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    SensorOrientationChangeNotifier.Listener {

    private val application: App
        get() = applicationContext as App

    private lateinit var binding: ActivityMainBinding

    private val cameraPermission = arrayOf(Manifest.permission.CAMERA)

    lateinit var previewView: PreviewView
    lateinit var previewContainer: ConstraintLayout
    lateinit var bottomOverlay: View

    // Hold a reference to the manual permission dialog to avoid re-creating it if it
    // is already visible and to dismiss it if the permission gets granted.
    private var cameraPermissionDialog: AlertDialog? = null
    private var audioPermissionDialog: AlertDialog? = null
    private var lastFrame: Bitmap? = null

    private lateinit var mainFrame: View
    lateinit var rootView: View

    lateinit var qrScanToggles: View
    private lateinit var moreOptionsToggle: View

    lateinit var qrToggle: QRToggle
    lateinit var dmToggle: QRToggle
    lateinit var cBToggle: QRToggle
    lateinit var azToggle: QRToggle

    lateinit var imageCapturer: ImageCapturer
    lateinit var videoCapturer: VideoCapturer

    lateinit var flipCameraCircle: View
    lateinit var cancelButtonView: ImageView
    lateinit var tabLayout: BottomTabLayout
    lateinit var thirdCircle: ImageView
    lateinit var captureButton: ImageButton

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var dbTapGestureDetector: GestureDetector
    lateinit var timerView: TextView
    lateinit var thirdOption: View
    lateinit var imagePreview: ShapeableImageView
    lateinit var previewLoader: ProgressBar
    private var isZooming = false

    lateinit var zoomBar: ZoomBar
    lateinit var zoomBarPanel: LinearLayout

    lateinit var exposureBar: ExposureBar
    lateinit var exposureBarPanel: LinearLayout

    lateinit var qrOverlay: QROverlay

    lateinit var threeButtons: LinearLayout

    lateinit var settingsIcon: ImageView

    private lateinit var exposurePlusIcon: ImageView
    private lateinit var exposureNegIcon: ImageView

    private lateinit var zoomInIcon: ImageView
    private lateinit var zoomOutIcon: ImageView

    lateinit var flipCamIcon: ImageView

    lateinit var mainOverlay: ImageView

    lateinit var settingsDialog: SettingsDialog
    lateinit var previewGrid: CustomGrid

    private var wasSwiping = false

    lateinit var cdTimer: CountDownTimerUI
    var timerDuration = 0

    lateinit var cbText: TextView
    lateinit var cbCross: ImageView

    lateinit var gCircleFrame: FrameLayout

    private lateinit var gAngleTextView: TextView
    private lateinit var gCircle: LinearLayout

    private lateinit var gLineX: View
    private lateinit var gLineZ: View

    private lateinit var gLeftDash: View
    private lateinit var gRightDash: View

    lateinit var muteToggle: ShapeableImageView

    private var bottomNavigationBarPadding: Int = 0

    private var shouldGyroVibrate = true
    private var hasGyroVibrated = false

    private val gyroVibRunnable = Runnable {
        vibrateDevice()
        hasGyroVibrated = true
    }

    val thumbnailLoaderExecutor = Executors.newSingleThreadExecutor()

    private val runnable = Runnable {
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            previewView.width.toFloat(), previewView.height.toFloat()
        )

        val autoFocusPoint = factory.createPoint(
            previewView.width / 2.0f,
            previewView.height / 2.0f, QROverlay.RATIO
        )

        camConfig.camera?.cameraControl?.startFocusAndMetering(
            FocusMeteringAction.Builder(autoFocusPoint).disableAutoCancel().build()
        )

        startFocusTimer()
    }

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var snackBar: Snackbar

    private lateinit var focusRing: ImageView

    private val focusRingHandler: Handler = Handler(Looper.getMainLooper())
    private val focusRingCallback: Runnable = Runnable {
        focusRing.visibility = View.INVISIBLE
    }

    lateinit var micOffIcon: ImageView

    private var shouldRestartRecording = false

    fun startFocusTimer() {
        handler.postDelayed(runnable, autoCenterFocusDuration)
    }

    fun cancelFocusTimer() {
        handler.removeCallbacks(runnable)
    }

    private val restartRecordingWithAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            shouldRestartRecording = true
            camConfig.startCamera(true)
            return@registerForActivityResult
        }
        showAudioPermissionDeniedDialog {
            videoCapturer.startRecording()
        }
    }

    // Used to request permission from the user
    private val requestPermissionLauncher = registerForActivityResult(
        RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        if (permissions.containsKey(Manifest.permission.RECORD_AUDIO)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted for recording audio.")
            } else {
                Log.i(TAG, "Permission denied for recording audio.")
                showAudioPermissionDeniedDialog()
            }
        }
        if (permissions.containsKey(Manifest.permission.CAMERA)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted for camera.")
            } else {
                Log.i(TAG, "Permission denied for camera.")
            }
        }
    }

    // Used to request permission from the user
    var dirPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->

        val data: Uri? = result.data?.data

        if (data?.encodedPath != null) {
            val file = File(data.encodedPath!!)
            if (file.exists()) {
                showMessage(getString(R.string.file_already_exists, file.absolutePath))
            } else {
                showMessage(getString(R.string.file_does_not_exist, data.encodedPath))
            }
        }

        Log.i(TAG, "Selected location: ${data?.encodedPath!!}")
    }

    private fun showAudioPermissionDeniedDialog(onDisableAudio: () -> Unit = {}) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.audio_permission_dialog_title)
            .setMessage(R.string.audio_permission_dialog_message)

        // Open the settings menu for the current app
        builder.setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts(
                "package",
                packageName, null
            )
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.cancel, null)

        builder.setNeutralButton(R.string.disable_audio) { _: DialogInterface?, _: Int ->
            camConfig.includeAudio = false
            onDisableAudio()
        }

        audioPermissionDialog = builder.showIgnoringShortEdgeMode()
    }

    fun updateLastFrame() {
        lastFrame = previewView.bitmap
    }

    private fun animateFocusRing(x: Float, y: Float) {

        // Move the focus ring so that its center is at the tap location (x, y)
        val width = focusRing.width.toFloat()
        focusRing.updateLayoutParams<ConstraintLayout.LayoutParams> {
            updateMargins(
                left = (x - width / 2).roundToInt(),
                top = (y - width / 2).roundToInt()
            )
        }

        // Show focus ring
        focusRing.visibility = View.VISIBLE
        focusRing.alpha = 1f

        if (areSystemAnimationsEnabled()) {
            // Animate the focus ring to disappear
            focusRing.animate()
                .setStartDelay(500)
                .setDuration(300)
                .alpha(0f)
                .setListener(object : Animator.AnimatorListener {

                    var isCancelled = false

                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animator: Animator) {

                        if (!isCancelled) {
                            focusRing.visibility = View.INVISIBLE
                        }

                        isCancelled = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        isCancelled = true
                    }

                    override fun onAnimationRepeat(animation: Animator) {}
                }).start()
        } else {
            focusRingHandler.removeCallbacks(focusRingCallback)
            focusRingHandler.postDelayed(focusRingCallback, 800)
        }
    }

    private fun areSystemAnimationsEnabled(): Boolean {

        val duration: Float = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )

        val transition: Float = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE, 1f
        )

        return duration != 0f && transition != 0f
    }

    private fun openGallery() {
        check(this !is CaptureActivity)

        Intent(this, InAppGallery::class.java).let {
            if (this is SecureMainActivity) {
                it.putExtra(InAppGallery.INTENT_KEY_SECURE_MODE, true)

                val list = capturedItems
                if (list.isEmpty()) {
                    showMessage(R.string.no_image)
                    return
                }
                it.putParcelableArrayListExtra(InAppGallery.INTENT_KEY_LIST_OF_SECURE_MODE_CAPTURED_ITEMS, list)
            } else {
                it.putExtra(InAppGallery.INTENT_KEY_VIDEO_ONLY_MODE, requiresVideoModeOnly)
            }

            if (isThumbnailLoaded) { // indicates that last captured item is accessible
                it.putExtra(InAppGallery.INTENT_KEY_LAST_CAPTURED_ITEM, camConfig.lastCapturedItem)
            }

            startActivity(it)
        }

    }

    private fun checkPermissions() {
        Log.i(TAG, "Checking camera status...")

        // Check if the app has access to the user's camera
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) ==
                    PackageManager.PERMISSION_GRANTED -> {

                // If the user has manually granted the permission, dismiss the dialog.
                if (cameraPermissionDialog != null && cameraPermissionDialog!!.isShowing) cameraPermissionDialog!!.cancel()
                Log.i(TAG, "Permission granted.")

                // Setup the camera since the permission is available
                camConfig.initializeCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {

                Log.i(TAG, "The user has default denied camera permission.")

                // Don't build and show a new dialog if it's already visible
                if (cameraPermissionDialog != null && cameraPermissionDialog!!.isShowing) return
                val builder = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.camera_permission_dialog_title)
                    .setMessage(R.string.camera_permission_dialog_message)
                val positiveClicked = AtomicBoolean(false)

                // Open the settings menu for the current app
                builder.setPositiveButton(R.string.settings) { _: DialogInterface?, _: Int ->
                    positiveClicked.set(true)
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(
                        "package",
                        packageName, null
                    )
                    intent.data = uri
                    startActivity(intent)
                }
                builder.setNegativeButton(R.string.cancel, null)
                builder.setOnDismissListener {

                    // The dialog could have either been dismissed by clicking on the
                    // background or by clicking the cancel button. So in those cases,
                    // the app should exit as the app depends on the camera permission.
                    if (!positiveClicked.get()) {
                        finish()
                    }
                }
                cameraPermissionDialog = builder.showIgnoringShortEdgeMode()
            }

            // Request for the permission (Android will actually popup the permission
            // dialog in this case)
            else -> {
                Log.i(TAG, "Requesting permission from user...")

                requestPermissionLauncher.launch(cameraPermission)
            }
        }

        audioPermissionDialog?.let { dialog ->
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // there are no camera controls in qr mode
        if (camConfig.isQRMode) {
            return super.onKeyUp(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_CAMERA -> {
                captureButton.performClick()
            }
            KeyEvent.KEYCODE_FOCUS -> {
                // cancel any manual focus
                // CameraX will start the continuous autofocus (if supported) automatically
                previewView.controller?.cameraControl?.cancelFocusAndMetering()
            }
            KeyEvent.KEYCODE_ZOOM_IN -> {
                cameraControl.zoomIn()
            }
            KeyEvent.KEYCODE_ZOOM_OUT -> {
                cameraControl.zoomOut()
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Pretend as if the event was handled by the app (avoid volume bar from appearing)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        resumeOrientationSensor()
        // Check camera permission again if the user switches back to the app (maybe
        // after enabling/disabling the camera permission in Settings)
        // Will also be called by Android Lifecycle when the app starts up
        checkPermissions()

        if (camConfig.isQRMode) {
            startFocusTimer()
        }

        if (this !is SecureActivity) {
            camConfig.fetchLastCapturedItemFromSharedPrefs()
        }

        updateThumbnail()

        if (camConfig.requireLocation) {
            requestLocation()
        }

        // If the preview of video capture activity isn't showing
        if (!(this is VideoCaptureActivity && thirdOption.visibility == View.VISIBLE)) {
            if (!isQRDialogShowing) {
                camConfig.initializeCamera(true)
            }
        }
    }

    val requiresVideoModeOnly: Boolean
        get() {
            return this is VideoOnlyActivity || this is VideoCaptureActivity
        }

    override fun onPause() {
        super.onPause()
        pauseOrientationSensor()
        if (camConfig.isQRMode) {
            cancelFocusTimer()
        } else {
            imageCapturer.cancelPendingCaptureRequest()
        }
        lastFrame = null
    }

    lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        snackBar = Snackbar.make(binding.root, "", Snackbar.LENGTH_LONG)

        gestureDetector = GestureDetector(this, this)

        camConfig = CamConfig(this)
        cameraControl = CameraControl(camConfig)
        mainOverlay = binding.mainOverlay
        imageCapturer = ImageCapturer(this)
        videoCapturer = VideoCapturer(this)
        thirdOption = binding.thirdOption
        previewLoader = binding.previewLoading
        imagePreview = binding.imagePreview
        previewView = binding.preview
        previewView.scaleType = PreviewView.ScaleType.FIT_START
        previewContainer = binding.previewContainer
        bottomOverlay = binding.bottomOverlay
        scaleGestureDetector = ScaleGestureDetector(this, this)
        dbTapGestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.i(TAG, "===============Double tap detected.=========")
//                val zoomState = config.camera!!.cameraInfo.zoomState.value
//                if (zoomState != null) {
//                    val start = zoomState.linearZoom
//                    var end = start * 1.5f
//                    if (end < 0.25f) end = 0.25f else if (end > zoomState.maxZoomRatio) end =
//                        zoomState.maxZoomRatio
//                    val animator = ValueAnimator.ofFloat(start, end)
//                    animator.duration = 300
//                    animator.addUpdateListener { valueAnimator: ValueAnimator ->
//                        config.camera!!.cameraControl.setLinearZoom(
//                            valueAnimator.animatedValue as Float
//                        )
//                    }
//                    animator.start()
//                }
                return super.onDoubleTap(e)
            }
        })

        tabLayout = binding.cameraModeTabs

        tabLayout.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                val tab = tabLayout.getTabAtX(tabLayout.scrollX)
                finalizeMode(tab)
                return@setOnTouchListener true
            }

            return@setOnTouchListener false
        }

        timerView = binding.timer
        previewView.previewStreamState.observe(this) { state: StreamState ->
            if (state == StreamState.STREAMING) {
                mainOverlay.visibility = View.INVISIBLE
                camConfig.reloadSettings()
                if (!camConfig.isQRMode) {
                    previewGrid.visibility = View.VISIBLE
                    if (!settingsDialog.isShowing) {
                        settingsIcon.visibility = View.VISIBLE
                    }
                    settingsIcon.isEnabled = true
                }

                restartRecordingIfPermissionsWasUnavailable()
            } else {
                previewGrid.visibility = View.INVISIBLE
                val lastFrame = lastFrame
                if (lastFrame != null && this !is CaptureActivity) {
                    setBlurBitmapCompat(mainOverlay, lastFrame)
                    settingsIcon.visibility = View.INVISIBLE
                    settingsIcon.isEnabled = false
                    mainOverlay.visibility = View.VISIBLE
                }
            }
        }
        flipCameraCircle = binding.flipCameraCircle

        flipCamIcon = binding.flipCameraIconContent

        var tapDownTimestamp: Long = 0
        flipCameraCircle.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (tapDownTimestamp == 0L) {
                        tapDownTimestamp = System.currentTimeMillis()
                        flipCameraCircle.animate().scaleXBy(0.05f).setDuration(300).start()
                        flipCameraCircle.animate().scaleYBy(0.05f).setDuration(300).start()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val dif = System.currentTimeMillis() - tapDownTimestamp
                    if (dif < 300) {
                        flipCameraCircle.performClick()
                    }

                    tapDownTimestamp = 0
                    flipCameraCircle.animate().cancel()
                    flipCameraCircle.animate().scaleX(1f).setDuration(300).start()
                    flipCameraCircle.animate().scaleY(1f).setDuration(300).start()
                }
                else -> {
                }
            }
            true
        }
        flipCameraCircle.setOnClickListener {
            resetAutoSleep()
            if (camConfig.isQRMode) {
                camConfig.scanAllCodes = !camConfig.scanAllCodes
                return@setOnClickListener
            }

            if (videoCapturer.isRecording) {
                videoCapturer.isPaused = !videoCapturer.isPaused
                return@setOnClickListener
            }

            val flipCameraIcon: ImageView = binding.flipCameraIcon
            val rotation: Float = if (flipCameraIcon.rotation < 180) {
                180f
            } else {
                360f
            }

            val rotate = RotateAnimation(
                0F,
                rotation,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate.duration = 400
            rotate.interpolator = LinearInterpolator()

            it.startAnimation(rotate)
            camConfig.toggleCameraSelector()
        }
        thirdCircle = binding.thirdCircle
        thirdCircle.setOnClickListener {
            resetAutoSleep()
            if (videoCapturer.isRecording) {
                imageCapturer.takePicture()
            } else {
                openGallery()
                Log.i(TAG, "Attempting to open gallery...")
            }
        }

        thirdCircle.setOnLongClickListener {
            if (videoCapturer.isRecording) {
                imageCapturer.takePicture()
            } else {
                shareLatestMedia()

            }

            return@setOnLongClickListener true
        }

        captureButton = binding.captureButton
        captureButton.setOnClickListener {
            resetAutoSleep()
            if (camConfig.isVideoMode) {
                if (videoCapturer.isRecording) {
                    videoCapturer.stopRecording()
                } else {
                    videoCapturer.startRecording()
                }
            } else if (camConfig.isQRMode) {
                camConfig.toggleTorchState()
                captureButton.setImageResource(
                    if (camConfig.isTorchOn) {
                        R.drawable.torch_on_button
                    } else {
                        R.drawable.torch_off_button
                    }
                )
            } else {
                if (timerDuration == 0) {
                    imageCapturer.takePicture()
                } else {
                    if (cdTimer.isRunning) {
                        cdTimer.cancelTimer()
                    } else {
                        cdTimer.startTimer()
                    }
                }
            }
        }

        cancelButtonView = binding.cancelButton
//        cancelButtonView.setOnClickListener(object : View.OnClickListener {
//
//            val SWITCH_ANIM_DURATION = 150
//            override fun onClick(v: View) {
//
//                val imgID = if (config.isVideoMode) R.drawable.video_camera else R.drawable.camera
//                config.switchCameraMode()
//                val oa1 = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0f)
//                val oa2 = ObjectAnimator.ofFloat(v, "scaleX", 0f, 1f)
//                oa1.interpolator = DecelerateInterpolator()
//                oa2.interpolator = AccelerateDecelerateInterpolator()
//                oa1.duration = SWITCH_ANIM_DURATION.toLong()
//                oa2.duration = SWITCH_ANIM_DURATION.toLong()
//                oa1.addListener(object : AnimatorListenerAdapter() {
//                    override fun onAnimationEnd(animation: Animator) {
//                        super.onAnimationEnd(animation)
//                        cancelButtonView.setImageResource(imgID)
//                        oa2.start()
//                    }
//                })
//                oa1.start()
//                if (config.isVideoMode) {
//                    captureButton.setImageResource(R.drawable.recording)
//                    cbText.visibility = View.INVISIBLE
//                } else {
//                    captureButton.setImageResource(R.drawable.camera_shutter)
//                    if (timerDuration != 0) {
//                        cbText.visibility = View.VISIBLE
//                    }
//                }
//            }
//        })

        zoomBar = binding.zoomBar
        zoomBar.setMainActivity(this)

        zoomBarPanel = binding.zoomBarPanel

        exposureBar = binding.exposureBar
        exposureBar.setMainActivity(this)

        exposureBarPanel = binding.exposureBarPanel

        qrOverlay = binding.qrOverlay

        threeButtons = binding.threeButtons
        settingsIcon = binding.settingsOption
        settingsIcon.setOnClickListener {
            if (!camConfig.isQRMode)
                settingsDialog.show()
        }

        settingsIcon.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val displayCutout = window.decorView.rootWindowInsets.displayCutout
                    val layoutParams = (settingsIcon.layoutParams as RelativeLayout.LayoutParams)

                    val rect = if (displayCutout?.boundingRects?.isNotEmpty() == true)
                        displayCutout.boundingRects.first() else null

                    val windowsSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        windowManager.currentWindowMetrics.bounds
                    } else {
                        val size = Point()
                        // defaultDisplay isn't deprecated below API 30 as highlighted by the IDE
                        // and this code would only execute if it is (Hint: enclosing if-block)
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay.getRealSize(size)
                        Rect(0, 0, size.x, size.y)
                    }

                    if (rect == null || rect.left <= 0 || rect.right == windowsSize.right) {
                        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    } else {
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    }
                }
            })

        exposurePlusIcon = binding.exposurePlusIcon
        exposureNegIcon = binding.exposureNegIcon

        zoomInIcon = binding.zoomInIcon
        zoomOutIcon = binding.zoomOutIcon

        previewGrid = binding.previewGrid
        previewGrid.setMainActivity(this)

        rootView = binding.root

        mainFrame = binding.mainFrame

        qrScanToggles = binding.qrScanToggles

        var isInsetSet = false

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())

            view.layoutParams = (view.layoutParams as ViewGroup.MarginLayoutParams).let {
                it.setMargins(
                    insets.left,
                    0,
                    insets.right,
                    0,
                )

                it
            }

            zoomBarPanel.setPadding(0, 0, 0, insets.bottom)
            exposureBarPanel.setPadding(0, 0, 0, insets.bottom)
            bottomNavigationBarPadding = insets.bottom

            if (insets.top != 0 && !isInsetSet) {
                mainFrame.layoutParams =
                    (mainFrame.layoutParams as ViewGroup.MarginLayoutParams).let {
                        it.setMargins(
                            it.leftMargin,
                            (8 * resources.displayMetrics.density.toInt()) + insets.top,
                            it.rightMargin,
                            it.bottomMargin,
                        )

                        it
                    }

                qrScanToggles.layoutParams =
                    (qrScanToggles.layoutParams as ViewGroup.MarginLayoutParams).let {
                        it.setMargins(
                            it.leftMargin,
                            (16 * resources.displayMetrics.density.toInt()) +
                                    insets.top,
                            it.rightMargin,
                            it.bottomMargin,
                        )

                        it
                    }

                isInsetSet = true
            }

            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.getRoot())

        WindowInsetsControllerCompat(window, rootView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        enableEdgeToEdge()

        cdTimer = binding.cTimer
        cdTimer.setMainActivity(this)

        cbText = binding.captureButtonText
        cbCross = binding.captureButtonCross

        val themedContext = DynamicColors.wrapContextIfAvailable(this, R.style.Theme_SettingsDialog)
        settingsDialog = SettingsDialog(this, themedContext)

        SystemSettingsObserver(lifecycle,Settings.System.ACCELEROMETER_ROTATION,this) {
            forceUpdateOrientationSensor()
        }

        focusRing = binding.focusRing

        micOffIcon = binding.micOff

        previewView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    previewView.viewTreeObserver.removeOnPreDrawListener(this)
                    repositionTabLayout()
                    return true
                }
            }
        )

        moreOptionsToggle = binding.moreOptions
        moreOptionsToggle.setOnClickListener {
            camConfig.showMoreOptionsForQR()
        }

        qrToggle = binding.qrScanToggle
        qrToggle.mActivity = this
        qrToggle.key = BarcodeFormat.QR_CODE.name

        dmToggle = binding.dataMatrixToggle
        dmToggle.mActivity = this
        dmToggle.key = BarcodeFormat.DATA_MATRIX.name

        cBToggle = binding.pdf417Toggle
        cBToggle.mActivity = this
        cBToggle.key = BarcodeFormat.PDF_417.name

        azToggle = binding.aztecToggle
        azToggle.mActivity = this
        azToggle.key = BarcodeFormat.AZTEC.name

        camConfig.loadSettings()

        gCircle = binding.gCircle
        gAngleTextView = binding.gCircleText

        gLineX = binding.gCircleLineX
        gLineZ = binding.gCircleLineZ

        gLeftDash = binding.gCircleLeftDash
        gRightDash = binding.gCircleRightDash

        gCircleFrame = binding.gCircleFrame

        muteToggle = binding.muteToggle
        muteToggle.setOnClickListener {
            if (videoCapturer.isMuted) {
                videoCapturer.unmuteRecording()
                muteToggle.setImageResource(R.drawable.mic_on)
                muteToggle.setBackgroundColor(getColor(R.color.red))
                muteToggle.tooltipText = getString(R.string.tap_to_mute_audio)
                showMessage(R.string.video_audio_recording_unmuted)
            } else {
                videoCapturer.muteRecording()
                muteToggle.setImageResource(R.drawable.mic_off)
                muteToggle.setBackgroundColor(getColor(android.R.color.darker_gray))
                muteToggle.tooltipText = getString(R.string.tap_to_unmute_audio)
                showMessage(R.string.video_audio_recording_muted)
            }
        }
    }

    private fun repositionTabLayout() {

        threeButtons.visibility = View.VISIBLE

        tabLayout.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {

                    tabLayout.viewTreeObserver
                        .removeOnPreDrawListener(
                            this
                        )

                    val previewHeight169 = previewContainer.width * 16 / 9

                    val extraHeight169 = previewContainer.height -
                            previewHeight169 -
                            tabLayout.height -
                            10 * resources.displayMetrics.density.toInt()

                    tabLayout.layoutParams =
                        (tabLayout.layoutParams as ViewGroup.MarginLayoutParams).let {

                            it.setMargins(
                                it.leftMargin,
                                it.topMargin,
                                it.rightMargin,
                                if (extraHeight169 > 0) {
                                    extraHeight169
                                } else {
                                    bottomNavigationBarPadding
                                }
                            )

                            it
                        }

                    return true
                }

            })
    }

    fun finalizeMode(tab: TabLayout.Tab? = null) {

        val selectedTab = tab ?: tabLayout.selectedTab
        if (selectedTab != null) {
            val mode = selectedTab.tag as CameraMode
            tabLayout.centerTab(selectedTab)
            tab?.let { tabLayout.centerTab(it) }
            camConfig.switchMode(mode)
            resetAutoSleep()
        }
    }

    fun restartRecordingWithMicPermission() {
        restartRecordingWithAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun shareLatestMedia() {
        val item = camConfig.lastCapturedItem
        if (item == null) {
            showMessage(R.string.please_wait_for_image_to_get_captured_before_sharing)
            return
        }

        Intent(Intent.ACTION_SEND).let {
            it.putExtra(Intent.EXTRA_STREAM, item.uri)
            it.setDataAndType(item.uri, item.mimeType())
            startActivity(Intent.createChooser(it, getString(R.string.share_image)))
        }
    }

    open fun bytesToHex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "" // outLen will be wrong for empty inputs

        // Represent bytes as a grid of hex digits:
        // Add a space between every byte
        // Double space every 4 bytes (unless end or newline)
        // Add a newline every 8 bytes (unless end)

        var outLen = bytes.size * 3 - 1 // 2 hex digits + 1 space/newline per byte (except last)
        outLen += bytes.size / 8 // One double space per row except the last incomplete row
        if (bytes.size % 8 > 4) {
            outLen += 1 // One double space for the last incomplete row, if it has >4 columns
        }

        val hexChars = CharArray(outLen)
        var j = 0 // Output index

        for (i in bytes.indices) {
            val byte = bytes[i].toInt() and 0xFF
            hexChars[j++] = hexArray[byte ushr 4]
            hexChars[j++] = hexArray[byte and 0x0F]

            if (i == bytes.lastIndex) break // No trailing whitespace
            if (i % 8 == 7) {
                hexChars[j++] = '\n'
            } else {
                hexChars[j++] = ' '
                if (i % 4 == 3) hexChars[j++] = ' '
            }
        }

        return String(hexChars)
    }

    private var isQRDialogShowing = false

    fun onScanResultSuccess(rawText: String) {

        if (isQRDialogShowing) return

        isQRDialogShowing = true

        val hString = bytesToHex(
            rawText.toByteArray(StandardCharsets.UTF_8)
        )

        runOnUiThread {
            val builder = MaterialAlertDialogBuilder(this)
            val dialogBinding = ScanResultDialogBinding.inflate(layoutInflater)
            builder.setView(dialogBinding.root)

            val tabLayout: TabLayout = dialogBinding.encodingTabs
            val textView = dialogBinding.scanResultText

            val intentView = Intent(Intent.ACTION_VIEW, Uri.parse(rawText))

            if (packageManager.resolveActivity(intentView, 0L) != null) {
                dialogBinding.openWith.setOnClickListener {
                    val chooser = Intent.createChooser(intentView, getString(R.string.open_with))
                    startActivity(chooser)
                }
            } else {
                dialogBinding.openWith.visibility = View.GONE
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.text.toString()) {
                        "Binary" -> {
                            textView.autoLinkMask = 0
                            textView.text = hString
                        }

                        "UTF-8" -> {
                            textView.autoLinkMask =
                                Linkify.WEB_URLS or Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES
                            textView.text = rawText
                        }
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
            })

            tabLayout.addTab(tabLayout.newTab().apply {
                text = "UTF-8"
            })

            tabLayout.addTab(tabLayout.newTab().apply {
                text = "Binary"
            })

            val ctc: ImageButton = dialogBinding.copyQrText
            ctc.setOnClickListener {
                val clipboardManager = getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager
                val clipData = ClipData.newPlainText(
                    "text",
                    textView.text
                )
                clipboardManager.setPrimaryClip(clipData)

                showMessage(getString(R.string.copied_text_to_clipboard))
            }

            val sButton: ImageButton = dialogBinding.shareQrText
            sButton.setOnClickListener {
                val sIntent = Intent(Intent.ACTION_SEND)
                sIntent.type = "text/plain"
                sIntent.putExtra(Intent.EXTRA_TEXT, textView.text.toString())
                startActivity(
                    Intent.createChooser(
                        sIntent,
                        getString(R.string.share_text_via)
                    )
                )
            }

            builder.setOnDismissListener {
                isQRDialogShowing = false
                camConfig.startCamera(true)
            }

            camConfig.cameraProvider?.unbindAll()

            builder.showIgnoringShortEdgeMode()
        }
    }

//    private val fTHandler : Handler = Handler(Looper.getMainLooper())
//    private val fTRunnable : Runnable = Runnable {
//        config.mPlayer.playFocusCompleteSound()
//    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        dbTapGestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_DOWN) return true else if (event.action == MotionEvent.ACTION_UP) {

            if (wasSwiping) {
                wasSwiping = false
                return wasSwiping
            }

            if (isZooming) {
                isZooming = false
                return true
            }

            if (camConfig.isQRMode)
                return false

            val x = event.x
            val y = event.y

            val autoFocusPoint = previewView.meteringPointFactory.createPoint(x, y)
            animateFocusRing(x, y)

            val focusBuilder = FocusMeteringAction.Builder(autoFocusPoint)

            if (!camConfig.isVideoMode) {
                camConfig.mPlayer.playFocusStartSound()
            }

            if (camConfig.focusTimeout == 0L) {
                focusBuilder.disableAutoCancel()
            } else {
                focusBuilder.setAutoCancelDuration(camConfig.focusTimeout, TimeUnit.SECONDS)
//                fTHandler.removeCallbacks(fTRunnable)
//                fTHandler.postDelayed(fTRunnable, focusTimeout * 1000)
            }

            camConfig.camera!!.cameraControl.startFocusAndMetering(focusBuilder.build())

            exposureBar.showPanel()
            zoomBar.showPanel()
            return v.performClick()
        }
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        isZooming = true
        val zoomState = camConfig.camera!!.cameraInfo.zoomState.value
        var scale = 1f
        if (zoomState != null) {
            scale = zoomState.zoomRatio * detector.scaleFactor
        }
        camConfig.camera!!.cameraControl.setZoomRatio(scale)
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    private fun rotateView(view: View?, angle: Float) {
        if (view != null) {
            view.animate().cancel()

            // Ensuring that the rotation seems continuous
            if (view.rotation == 0f && angle == 270f)
                view.rotation = 360f

            if (view.rotation == 270f && angle == 0f)
                view.rotation = -90f

            view.animate()
                .rotation(angle)
                .setDuration(400)
                .setInterpolator(LinearInterpolator())
                .start()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onOrientationChange(orientation: Int) {

        val tr = when (orientation) {
            in 45..134 -> Surface.ROTATION_270
            in 135..224 -> Surface.ROTATION_180
            in 225..314 -> Surface.ROTATION_90
            else -> Surface.ROTATION_0
        }

        camConfig.imageCapture?.targetRotation = tr
        camConfig.videoCapture?.targetRotation = tr
        camConfig.iAnalyzer?.targetRotation = tr

        if (videoCapturer.isRecording) return

        var iconRotation = (360f - orientation) % 360

        // Rotate views that should rotate irrespective of the auto-rotate setting
        rotateView(gCircleFrame, iconRotation)

        // Set iconRotation to 0
        if (Settings.System.getInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0
            ) != 1
        ) {
            iconRotation = 0f
        }

        // Rotate views that shouldn't be affected by the auto rotate setting
        // (Rotates back to 0 when the auto rotate gets toggled to off when the app
        // is running)
        rotateView(flipCameraCircle, iconRotation)
        rotateView(cancelButtonView, iconRotation)
        rotateView(thirdOption, iconRotation)

        rotateView(exposurePlusIcon, iconRotation)
        rotateView(exposureNegIcon, iconRotation)
        rotateView(zoomInIcon, iconRotation)
        rotateView(zoomOutIcon, iconRotation)
        rotateView(settingsDialog.settingsFrame, iconRotation)

        rotateView(micOffIcon, iconRotation)
        rotateView(muteToggle, iconRotation)
    }

    lateinit var camConfig: CamConfig
    private lateinit var cameraControl: CameraControl

    companion object {
        private const val TAG = "GOCam"
        private const val autoCenterFocusDuration = 2000L
        private val hexArray = "0123456789ABCDEF".toCharArray()

        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100

        private const val GYRO_VIBE_WAIT_TIME = 250L
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {

        var result = false
        try {
            e1 ?: return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    result = true
                }
            } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeBottom()
                } else {
                    onSwipeTop()
                }
                result = true
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return result
    }

    private fun onSwipeBottom() {
        if (isZooming || cdTimer.isRunning) return
        wasSwiping = true
        if (settingsDialog.isShowing) return

        if (camConfig.isQRMode) {
            if (!camConfig.scanAllCodes) {
                camConfig.showMoreOptionsForQR()
            }
        } else {
            if (settingsIcon.isEnabled) {
                settingsIcon.performClick()
            }
        }
    }

    private fun onSwipeRight() {

        if (isZooming || cdTimer.isRunning || videoCapturer.isRecording)
            return

        if (this is VideoOnlyActivity) return

        wasSwiping = true
        if (settingsDialog.isShowing) return


        val i = tabLayout.selectedTabPosition - 1

        vibrateDevice()

        Log.i(TAG, "onSwipeRight $i")
        tabLayout.getTabAt(i)?.let {
            finalizeMode(it)
        }
    }

    private fun onSwipeTop() {
        if (isZooming || cdTimer.isRunning || videoCapturer.isRecording) return
        wasSwiping = true
        settingsDialog.slideDialogUp()
    }

    private fun onSwipeLeft() {
        if (isZooming || cdTimer.isRunning) return

        if (this is VideoOnlyActivity) return

        wasSwiping = true
        if (settingsDialog.isShowing) return

        vibrateDevice()

        val i = tabLayout.selectedTabPosition + 1
        tabLayout.getTabAt(i)?.let {
            finalizeMode(it)
        }
    }

    override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(p0: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTapEvent(p0: MotionEvent): Boolean {
        return false
    }

    fun showMessage(@StringRes msg: Int, action: String? = null, callback: View.OnClickListener? = null) {
        showMessage(getString(msg), action, callback)
    }

    fun showMessage(msg: String, action: String? = null, callback: View.OnClickListener? = null) {
        snackBar.apply {
            setText(msg)
            setAction(action, callback)
            show()
        }
    }


    fun indicateLocationProvidedIsDisabled() {
        showMessage(getString(R.string.location_is_disabled),
            if (this !is SecureMainActivity) getString(R.string.enable) else null
        ) {
            enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun pauseOrientationSensor() {
        SensorOrientationChangeNotifier
            .getInstance(this)?.remove(this)
    }

    private fun resumeOrientationSensor() {
        sensorNotifier?.addListener(this)
    }

    fun forceUpdateOrientationSensor() {
        sensorNotifier?.notifyListeners(true)
    }

    val sensorNotifier: SensorOrientationChangeNotifier?
        get() {
            return SensorOrientationChangeNotifier.getInstance(this)
        }

    fun getRotation(): Int {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

        return when (rotation) {
            Surface.ROTATION_90 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 90
            else -> 0
        }
    }

    fun onDeviceAngleChange(xAngle: Float, zAngle: Float) {
        // If we are in photo mode and the countdown timer isn't running
        if (!(camConfig.isVideoMode || camConfig.isVideoMode || cdTimer.isRunning)) {

            if (gCircle.rotation != xAngle) {
                gCircle.rotation = xAngle
                gLineZ.rotation = xAngle

                val absXAngle = abs(xAngle).toInt()

                gAngleTextView.text = getString(R.string.degree_format, absXAngle)
                if (xAngle == 0f) {
                    setThicknessOfGLines(4)
                    if (shouldGyroVibrate) {
                        shouldGyroVibrate = false
                        hasGyroVibrated = false
                        handler.postDelayed(gyroVibRunnable, GYRO_VIBE_WAIT_TIME)
                    }
                } else {
                    handler.removeCallbacks(gyroVibRunnable)
                    if (!hasGyroVibrated || absXAngle > 5) {
                        shouldGyroVibrate = true
                    }
                    setThicknessOfGLines(2)
                }
            }

            Log.i(TAG, "zAngle: $zAngle")

            val lzAngle = when {
                zAngle < -45 -> {
                    -45
                }
                zAngle > 45 -> {
                    45
                }
                else -> {
                    zAngle
                }
            }.toFloat()

            if (zAngle.toInt() == 0) {
                gLineX.setBackgroundResource(R.drawable.yellow_shadow_rect)
                gLineZ.visibility = View.GONE

                gLeftDash.setBackgroundResource(R.drawable.yellow_shadow_rect)
                gRightDash.setBackgroundResource(R.drawable.yellow_shadow_rect)

                gAngleTextView.setTextColor(ContextCompat.getColor(this, R.color.z_yellow))

            } else {
                gLineX.setBackgroundResource(R.drawable.white_shadow_rect)
                gLineZ.visibility = View.VISIBLE

                gLeftDash.setBackgroundResource(R.drawable.white_shadow_rect)
                gRightDash.setBackgroundResource(R.drawable.white_shadow_rect)

                gAngleTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }

            val zOffset = (lzAngle / 60) * dp32

            gLineZ.layoutParams = (gLineZ.layoutParams as ViewGroup.MarginLayoutParams).let {
                it.setMargins(
                    it.leftMargin,
                    it.topMargin,
                    it.rightMargin,
                    zOffset.toInt(),
                )
                it
            }
        }
    }

    private val dp32 by lazy {
        32 * resources.displayMetrics.density
    }

    private fun setThicknessOfGLines(dp: Int) {
        val t = dp * resources.displayMetrics.density

        gLeftDash.layoutParams = gLeftDash.layoutParams.let {
            it.height = t.toInt()
            it
        }

        gRightDash.layoutParams = gRightDash.layoutParams.let {
            it.height = t.toInt()
            it
        }

        gLineX.layoutParams = gLineX.layoutParams.let {
            it.height = t.toInt()
            it
        }
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

    override fun onDestroy() {
        super.onDestroy()
        SensorOrientationChangeNotifier.clearInstance()
        thumbnailLoaderExecutor.shutdownNow()
    }

    fun locationCamConfigChanged(required: Boolean) {
        if (required) {
            requestLocation()
        } else {
            application.dropLocationUpdates()
        }
    }

    private val enableLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestLocation(application.isAnyLocationProvideActive())
    }

    // Used to request permission from the user
    private val locationPermissionLauncher = registerForActivityResult(
        RequestMultiplePermissions()
    ) {
        if (!application.shouldAskForLocationPermission()) {
            requestLocation()
        } else {
            camConfig.requireLocation = false
        }
    }

    private fun requestLocation(reAttach: Boolean = false) {

        when {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) && ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) -> {
                MaterialAlertDialogBuilder(this).let {
                    it.setTitle(R.string.location_permission_dialog_title)
                    it.setMessage(R.string.location_permission_dialog_message)

                    if (this !is SecureActivity) {
                        it.setPositiveButton(R.string.settings) { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", packageName, null)
                            this.startActivity(intent)
                        }
                    }

                    it.setOnDismissListener {
                        if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            camConfig.requireLocation = false
                        }
                    }
                }.showIgnoringShortEdgeMode()
            }

            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
                    == PackageManager.PERMISSION_GRANTED) -> {
                application.requestLocationUpdates(reAttach)
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    private fun resetAutoSleep() {
        application.resetPreventScreenFromSleeping()
    }

    @Volatile var isStarted = false

    override fun onStart() {
        super.onStart()
        isStarted = true
    }

    override fun onStop() {
        super.onStop()
        isStarted = false
        if (this::videoCapturer.isInitialized && videoCapturer.isRecording) {
            videoCapturer.stopRecording()
        }
    }

    var isThumbnailLoaded = false

    fun updateThumbnail() {
        val item = camConfig.lastCapturedItem
        val preview = imagePreview
        preview.setImageBitmap(null)
        isThumbnailLoaded = false

        if (item == null) {
            return
        }

        val ctx = applicationContext

        thumbnailLoaderExecutor.executeIfAlive {
            var bitmap: Bitmap? = null
            try {
                val side = preview.layoutParams.width

                if (item.type == ITEM_TYPE_VIDEO) {
                    val origBitmap = getVideoThumbnail(ctx, item.uri)
                    origBitmap?.let {
                        val w = it.width.toDouble()
                        val h = it.height.toDouble()
                        val ratio = max(w / side, h / side)

                        bitmap = Bitmap.createScaledBitmap(it, (w / ratio).toInt(), (h / ratio).toInt(), true)
                        origBitmap.recycle()
                    }
                } else if (item.type == ITEM_TYPE_IMAGE) {
                    val source = ImageDecoder.createSource(ctx.contentResolver, item.uri)
                    bitmap = ImageDecoder.decodeBitmap(source, ImageResizer(side, side))
                }
            } catch (e: Exception) {
                Log.d(TAG, "unable to update preview", e)
            }

            if (bitmap != null) {
                mainExecutor.execute {
                    if (isStarted && camConfig.lastCapturedItem == item) {
                        preview.setImageBitmap(bitmap)
                        isThumbnailLoaded = true
                    }
                }
            }
        }
    }

    open fun shouldShowCameraModeTabs() = true

    private fun restartRecordingIfPermissionsWasUnavailable() {
        if (shouldRestartRecording) {
            shouldRestartRecording = false
            videoCapturer.startRecording()
        }
    }
}
