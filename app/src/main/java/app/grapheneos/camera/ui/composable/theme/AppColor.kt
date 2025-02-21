package app.grapheneos.camera.ui.composable.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import app.grapheneos.camera.R

object AppColor {
    val BackgroundColor = Color(0xff181c1f)
    val AppBarColor = Color(0x77000000)
}



@Composable
fun appColorScheme() : ColorScheme {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val darkTheme = isSystemInDarkTheme()

    return when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> darkColorScheme(
            primaryContainer = colorResource(R.color.system_accent1_500)
        )
        else -> lightColorScheme(
            primaryContainer = colorResource(R.color.system_accent1_500)
        )
    }
}