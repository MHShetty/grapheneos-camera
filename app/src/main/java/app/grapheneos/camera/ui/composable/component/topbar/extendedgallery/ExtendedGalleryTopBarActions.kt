package app.grapheneos.camera.ui.composable.component.topbar.extendedgallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.EGLSurfaceTexture.SecureMode
import app.grapheneos.camera.R
import app.grapheneos.camera.ktx.isDeviceLocked
import app.grapheneos.camera.ui.composable.component.topbar.TopBarAction

private const val TAG = "ExtendedGalleryActions"

enum class ExtendedGalleryActions {
    SELECT_MEDIA_ACTION,
    SHARE_SELECTED_ITEMS_ACTION,
    DELETE_SELECTED_ITEMS_ACTION,
    SELECT_ALL_ITEMS_ACTION,
    UNLOCK_DEVICE_ACTION,
}

@Composable
fun defaultActions(
    secureMode: Boolean = false
) : List<TopBarAction> {

    return buildList {
        if (secureMode) {
            add(
                TopBarAction(
                    id = ExtendedGalleryActions.UNLOCK_DEVICE_ACTION,
                    title = stringResource(R.string.unlock_device),
                    icon = Icons.Default.LockOpen
                )
            )
        }

        add(
            TopBarAction(
                id = ExtendedGalleryActions.SELECT_MEDIA_ACTION,
                title = stringResource(R.string.select_media),
                icon = Icons.Default.Check,
                alwaysInMoreOptions = true
            )
        )
    }


}

@Composable
fun selectionModeActions() : List<TopBarAction> {
    return listOf(
        TopBarAction(
            id = ExtendedGalleryActions.SELECT_ALL_ITEMS_ACTION,
            title = stringResource(R.string.select_media),
            icon = Icons.Default.SelectAll,
        ),
        TopBarAction(
            id = ExtendedGalleryActions.DELETE_SELECTED_ITEMS_ACTION,
            title = stringResource(R.string.delete_items),
            icon = Icons.Default.Delete,
        ),
        TopBarAction(
            id = ExtendedGalleryActions.SHARE_SELECTED_ITEMS_ACTION,
            title = stringResource(R.string.share_items),
            icon = Icons.Default.Share,
        ),
    )
}
