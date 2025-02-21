package app.grapheneos.camera.ktx

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context

fun Context.isDeviceLocked() : Boolean {
    val keyguardManager: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return keyguardManager.isKeyguardLocked
}

fun Context.requestDeviceUnlock() {
    assert(this is Activity) {
        "Please ensure that requestDeviceUnlock() is called by an activity context"
    }
    val keyguardManager: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    keyguardManager.requestDismissKeyguard(this as Activity, null)
}