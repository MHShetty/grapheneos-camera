package app.grapheneos.camera.ui.activities

import android.net.Uri
import android.os.Bundle

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import app.grapheneos.camera.ui.composable.screen.ui.VideoPlayerScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

import app.grapheneos.camera.AutoFinishOnSleep

import app.grapheneos.camera.util.getParcelableExtra

class VideoPlayer : AppCompatActivity() {

    companion object {
        const val TAG = "VideoPlayer"
        const val IN_SECURE_MODE = "isInSecureMode"
        const val VIDEO_URI = "videoUri"
    }

    private val autoFinisher = AutoFinishOnSleep(this)

    private var isSecureMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

        val intent = this.intent

        isSecureMode = intent.getBooleanExtra(IN_SECURE_MODE, false)

        if (isSecureMode) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            autoFinisher.start()
        }

        val uri = getParcelableExtra<Uri>(intent, VIDEO_URI)!!

        setContent {
            VideoPlayerScreen(
                mediaUri = uri,
                onExitAction = this::finish
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isSecureMode) {
            this.autoFinisher.stop()
        }
    }
}
