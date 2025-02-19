package app.grapheneos.camera.ui.composable.screen.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.R
import app.grapheneos.camera.ktx.header
import app.grapheneos.camera.ktx.requestDeviceUnlock
import app.grapheneos.camera.ui.composable.component.SnackBarMessageHandler
import app.grapheneos.camera.ui.composable.component.dialog.MultipleFileDeletionDialog
import app.grapheneos.camera.ui.composable.component.mediapreview.SQUARE_MEDIA_PREVIEW_SIZE
import app.grapheneos.camera.ui.composable.component.mediapreview.SquareMediaPreview
import app.grapheneos.camera.ui.composable.component.topbar.extendedgallery.ExtendedGalleryTopBar

import app.grapheneos.camera.ui.composable.screen.viewmodel.ExtendedGalleryViewModel
import app.grapheneos.camera.util.getHumanReadableDate

@Composable
fun ExtendedGalleryScreen(
    showMediaItemAction: (CapturedItem) -> Unit = {},
    onExitAction: () -> Unit = {}
) {
    val context = LocalContext.current

    val viewModel = viewModel {
        ExtendedGalleryViewModel(context)
    }

    val snackBarHostState = remember {
        SnackbarHostState()
    }

    SnackBarMessageHandler(
        snackBarHostState = snackBarHostState,
        snackBarMessage = viewModel.snackBarMessage,
    )

    // Clear snackbar message on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.hideSnackBar()
        }
    }

    BackHandler {
        if (viewModel.selectMode) {
            viewModel.exitSelectionMode()
        } else {
            onExitAction()
        }
    }

    if (viewModel.isDeletionDialogVisible) {
        MultipleFileDeletionDialog(
            deletionItemsCount = viewModel.selectedItems.size,
            onDeleteConfirmationAction = {
                viewModel.deleteSelectedItems(context, onLastItemDeletion = onExitAction)
            },
            dismissCallback = viewModel::dismissDeletionDialog
        )
    }


    Scaffold (
        snackbarHost = {
            SnackbarHost(snackBarHostState)
        },

        topBar = {
            ExtendedGalleryTopBar(
                selectModeEnabled = viewModel.selectMode,
                secureMode = viewModel.isSecureCapturedItemsLoaded,
                selectedItems = viewModel.selectedItems,

                onEnableSelectMode = viewModel::enterSelectionMode,
                onDisableSelectMode = viewModel::exitSelectionMode,
                onAllItemsSelectAction = viewModel::selectAllItems,
                onDeleteItemsAction = {
                    viewModel.showDeletionDialog(context)
                },

                onSharedItemsAction = {
                    viewModel.shareSelectedItems(context)
                },

                onDeviceUnlockRequest = context::requestDeviceUnlock,

                onExitAction = onExitAction,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (viewModel.isLoadingCapturedItems) {
                Text(
                    stringResource(R.string.loading_generic),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                )
            } else {
                if (viewModel.hasCapturedItems) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(SQUARE_MEDIA_PREVIEW_SIZE),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        viewModel.groupedCapturedItems.forEach { group ->

                            val date = getHumanReadableDate(group.key)
                            val capturedItemsOnDate = group.value

                            header {
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            start = 14.dp,
                                            end = 14.dp,
                                            top = 16.dp,
                                            bottom = 8.dp,
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        date,
                                        style = MaterialTheme.typography.titleLarge,
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.toggleGroupSelection(capturedItemsOnDate)
                                        },
                                    ) {
                                        if (viewModel.selectMode) {
                                            if (viewModel.hasSelectedAll(capturedItemsOnDate)) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = null
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Outlined.Circle,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = null
                                            )
                                        }

                                    }
                                }
                            }

                            items(capturedItemsOnDate) {
                                    capturedItem ->
                                Box {
                                    SquareMediaPreview(
                                        capturedItem = capturedItem,
                                        onClick = {
                                            if (viewModel.selectMode) {
                                                viewModel.toggleSelection(capturedItem)
                                            } else {
                                                showMediaItemAction(capturedItem)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleSelection(capturedItem)
                                        }
                                    )

                                    if (viewModel.hasSelected(capturedItem)) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .fillMaxSize()
                                                .background(Color.Black.copy(.7f)),
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                } else {
                    Text(
                        stringResource(R.string.empty_gallery),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentHeight(align = Alignment.CenterVertically),

                    )
                }
            }
        }
    }
}