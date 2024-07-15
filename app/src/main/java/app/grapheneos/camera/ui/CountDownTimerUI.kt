package app.grapheneos.camera.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.camera.core.AspectRatio
import app.grapheneos.camera.CamConfig
import app.grapheneos.camera.ui.activities.CaptureActivity
import app.grapheneos.camera.ui.activities.MainActivity

class CountDownTimerUI @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private lateinit var timer: CountDownTimer
    lateinit var mActivity: MainActivity
    lateinit var camConfig: CamConfig

    companion object {
        private const val textAnimDuration = 700L

        const val startSize = 12f
        const val endSize = 100f
    }

    var isRunning = false
        private set

    fun setMainActivity(mainActivity: MainActivity) {
        this.mActivity = mainActivity
        camConfig = mainActivity.camConfig
    }

    fun startTimer() {
        cancelTimer()

        timer = object : CountDownTimer(mActivity.timerDuration * 1000L, 1000L) {
            override fun onTick(pendingMs: Long) {
                val pendingS = (pendingMs / 1000) + 1

                val scaleAnimation = ValueAnimator.ofFloat(startSize, endSize)
                scaleAnimation.interpolator = AccelerateDecelerateInterpolator()
                scaleAnimation.duration = textAnimDuration

                scaleAnimation.addUpdateListener { valueAnimator ->
                    textSize = valueAnimator.animatedValue as Float
                }

                val opacityAnimation = ValueAnimator.ofFloat(1f, 0f)
                opacityAnimation.interpolator = AccelerateDecelerateInterpolator()
                opacityAnimation.duration = textAnimDuration

                opacityAnimation.addUpdateListener { valueAnimator ->
                    alpha = valueAnimator.animatedValue as Float
                }

                scaleAnimation.start()
                opacityAnimation.start()

                text = pendingS.toString()

                if (text == "1") {
                    camConfig.mPlayer.playTimerFinalSSound()
                } else {
                    camConfig.mPlayer.playTimerIncrementSound()
                }
            }

            override fun onFinish() {
                onTimerEnd()
                if (mActivity is CaptureActivity) {
                    (mActivity as CaptureActivity).takePicture()
                } else {
                    mActivity.imageCapturer.takePicture()
                }
            }

        }

        beforeTimeStarts()

        timer.start()
    }

    fun cancelTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
            onTimerEnd(true)
        }
    }

    private fun beforeTimeStarts() {

        val params: ViewGroup.LayoutParams = layoutParams
        params.height = if (camConfig.getCurrentModeAspectRatio() == AspectRatio.RATIO_4_3) {
            mActivity.previewView.width * 4 / 3
        } else {
            mActivity.previewView.height
        }
        layoutParams = params

        mActivity.settingsIcon.visibility = View.INVISIBLE
        mActivity.thirdOption.visibility = View.INVISIBLE
        mActivity.flipCameraCircle.visibility = View.INVISIBLE
        mActivity.tabLayout.visibility = View.INVISIBLE
        mActivity.cancelButtonView.visibility = View.INVISIBLE
        mActivity.cbText.visibility = View.INVISIBLE
        mActivity.cbCross.visibility = View.VISIBLE

        visibility = View.VISIBLE
        isRunning = true
    }

    private fun onTimerEnd(isCancelled: Boolean = false) {
        mActivity.settingsIcon.visibility = View.VISIBLE
        mActivity.flipCameraCircle.visibility = View.VISIBLE
        mActivity.cancelButtonView.visibility = View.VISIBLE
        mActivity.cbCross.visibility = View.INVISIBLE

        if (mActivity !is CaptureActivity) {
            mActivity.cbText.visibility = View.VISIBLE
            mActivity.tabLayout.visibility = View.VISIBLE
            mActivity.thirdOption.visibility = View.VISIBLE
        } else if (isCancelled) {
            mActivity.cbText.visibility = View.VISIBLE
        }

        visibility = View.GONE
        isRunning = false
    }

    init {
        gravity = Gravity.CENTER
    }
}
