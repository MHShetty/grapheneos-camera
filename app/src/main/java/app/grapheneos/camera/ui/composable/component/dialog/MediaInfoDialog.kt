package app.grapheneos.camera.ui.composable.component.dialog

import androidx.compose.runtime.Composable

import androidx.compose.ui.res.stringResource
import app.grapheneos.camera.R
import app.grapheneos.camera.ui.composable.model.MediaItemDetails

@Composable
fun MediaInfoDialog(
    mediaItemDetails: MediaItemDetails?,
    dismissCallback : () -> Unit,
) {
    if (mediaItemDetails != null) {
        CommonInfoDialog(
            titleText = stringResource(R.string.file_details),
            message = """
            ${stringResource(R.string.file_name_generic)}
            ${mediaItemDetails.fileName ?: stringResource(id = R.string.not_found_generic)}

            ${stringResource(R.string.file_path)}
            ${mediaItemDetails.filePath ?: stringResource(id = R.string.not_found_generic)}

            ${stringResource(R.string.file_size)}
            ${mediaItemDetails.size ?: stringResource(id = R.string.not_found_generic)}

            ${stringResource(R.string.file_created_on)}
            ${mediaItemDetails.dateAdded ?: stringResource(id = R.string.not_found_generic)}

            ${stringResource(R.string.last_modified_on)}
            ${mediaItemDetails.dateModified ?: stringResource(id = R.string.not_found_generic)}
            """.trimIndent(),
            dismissCallback = dismissCallback
        )
    }
}