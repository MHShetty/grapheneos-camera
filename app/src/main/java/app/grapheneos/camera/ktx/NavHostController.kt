package app.grapheneos.camera.ktx

import androidx.navigation.NavHostController

fun NavHostController.popBackStack(
    onBackStackEmpty: () -> Unit
) {
    if (!popBackStack()) onBackStackEmpty()
}