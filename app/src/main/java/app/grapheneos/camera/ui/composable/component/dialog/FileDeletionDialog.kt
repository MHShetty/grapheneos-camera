package app.grapheneos.camera.ui.composable.component.dialog

import androidx.compose.runtime.Composable

import androidx.compose.ui.res.stringResource

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.R


@Composable
fun FileDeletionDialog(
    deletionItem : CapturedItem?,
    onDeleteAction: (item: CapturedItem) -> Unit,
    dismissHandler: () -> Unit
) {
    if (deletionItem != null) {
        CommonAlertDialog(
            titleText = stringResource(id = R.string.delete_title),
            message = stringResource(
                id = R.string.delete_description,
                deletionItem.uiName(),
            ),
            confirmationCallback = {
                onDeleteAction(deletionItem)
            },
            dismissCallback = dismissHandler,
        )
    }
}