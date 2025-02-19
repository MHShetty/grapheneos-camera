package app.grapheneos.camera.ui.composable.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.grapheneos.camera.ktx.dismissSnackBarIfVisible
import app.grapheneos.camera.ktx.showOrReplaceSnackbar
import app.grapheneos.camera.ui.composable.model.NoDataSnackBarMessage
import app.grapheneos.camera.ui.composable.model.SnackBarMessage

@Composable
fun SnackBarMessageHandler(
    snackBarHostState: SnackbarHostState,
    snackBarMessage: SnackBarMessage
) {
    LaunchedEffect(snackBarMessage) {
        if (snackBarMessage == NoDataSnackBarMessage) {
            snackBarHostState.dismissSnackBarIfVisible()
        } else {
            snackBarHostState.showOrReplaceSnackbar(snackBarMessage.message)
        }
    }
}