package app.grapheneos.camera.ui.composable.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.grapheneos.camera.R

@Composable
fun MultipleFileDeletionDialog(
    deletionItemsCount: Int,
    onDeleteConfirmationAction: () -> Unit,
    dismissCallback: () -> Unit
) = CommonAlertDialog(
    titleText = stringResource(R.string.delete_title),
    confirmButtonText = stringResource(R.string.delete),
    message = stringResource(R.string.multiple_deletion_message, deletionItemsCount),
    confirmationCallback = onDeleteConfirmationAction,
    dismissCallback = dismissCallback
)