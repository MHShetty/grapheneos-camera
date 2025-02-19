package app.grapheneos.camera.ui.composable.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun appTypography() : Typography {

    return Typography(
        titleSmall = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),

        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),

        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),

        bodySmall = MaterialTheme.typography.bodySmall.copy(
            letterSpacing = 0.sp,
        ),

        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            letterSpacing = 0.sp,
        ),

        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            letterSpacing = 0.sp,
        ),

        labelSmall = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.sp,
        ),

        labelMedium = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 0.sp,
        ),

        labelLarge = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 0.sp,
        ),

        displaySmall = MaterialTheme.typography.displaySmall.copy(
            letterSpacing = 0.sp,
        ),

        displayMedium = MaterialTheme.typography.displayMedium.copy(
            letterSpacing = 0.sp,
        ),

        displayLarge = MaterialTheme.typography.displayLarge.copy(
            letterSpacing = 0.sp,
        ),

        headlineSmall = MaterialTheme.typography.headlineSmall.copy(
            letterSpacing = 0.sp,
        ),

        headlineMedium = MaterialTheme.typography.headlineMedium.copy(
            letterSpacing = 0.sp,
        ),

        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            letterSpacing = 0.sp,
        ),
    )
}

