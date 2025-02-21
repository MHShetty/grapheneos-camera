package app.grapheneos.camera.ui.composable.component.topbar.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.grapheneos.camera.R
import app.grapheneos.camera.ui.composable.component.topbar.TopBarAction

private const val TAG = "GalleryActions"

enum class GalleryActions {
    EDIT_MEDIA,
    DELETE_MEDIA,
    SHOW_MEDIA_INFO,
    SHARE_MEDIA,
    EDIT_MEDIA_WITH_APP,
    EDIT_MEDIA_IN_PLACE,
}

@Composable
fun galleryTopBarActions() : List<TopBarAction> {
    return listOf(
        TopBarAction(
            id = GalleryActions.EDIT_MEDIA,
            title = stringResource(R.string.edit_a_copy),
            icon = Icons.Filled.Edit
        ),
        TopBarAction(
            id = GalleryActions.DELETE_MEDIA,
            title = stringResource(R.string.delete_media),
            icon = Icons.Filled.Delete
        ),
        TopBarAction(
            id = GalleryActions.SHOW_MEDIA_INFO,
            title = stringResource(R.string.show_media_info),
            icon = Icons.Filled.Info
        ),
        TopBarAction(
            id = GalleryActions.SHARE_MEDIA,
            title = stringResource(R.string.share_media),
            icon = Icons.Filled.Share
        ),
        TopBarAction(
            id = GalleryActions.EDIT_MEDIA_WITH_APP,
            title = stringResource(R.string.edit_a_copy_with),
            icon = Icons.Filled.Edit,
            alwaysInMoreOptions = true
        ),
        TopBarAction(
            id = GalleryActions.EDIT_MEDIA_IN_PLACE,
            title = stringResource(R.string.edit_original_image),
            icon = Icons.Filled.Edit,
            alwaysInMoreOptions = true
        ),
    )
}