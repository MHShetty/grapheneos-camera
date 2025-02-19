package app.grapheneos.camera.ui.composable.component.topbar.extendedgallery

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.R
import app.grapheneos.camera.ui.composable.component.topbar.TopBarActions

private const val TAG = "ExtendedGalleryTopBar"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendedGalleryTopBar(
    selectModeEnabled: Boolean,
    secureMode: Boolean = false,
    selectedItems: List<CapturedItem>,
    onEnableSelectMode: () -> Unit = {},
    onDisableSelectMode: () -> Unit = {},
    onAllItemsSelectAction : () -> Unit = {},
    onDeleteItemsAction: () -> Unit = {},
    onSharedItemsAction: () -> Unit = {},
    onDeviceUnlockRequest: () -> Unit = {},
    onExitAction: () -> Unit = {},
) {
    Surface(
        shadowElevation = 4.dp
    ) {
        TopAppBar(
            title = {
                if (selectModeEnabled) {
                    Text(
                        stringResource(R.string.selected_items_template, selectedItems.size),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },

            colors = TopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),

            navigationIcon = {
                if (selectModeEnabled) {
                    IconButton(onClick = onDisableSelectMode) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null
                        )
                    }
                } else {
                    IconButton(onClick = onExitAction) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },

            actions = {
                TopBarActions(
                    actions = if (selectModeEnabled) {
                        selectionModeActions()
                    } else {
                        defaultActions(secureMode = secureMode)
                    },

                    onActionClicked = { action ->
                        when (action) {
                            ExtendedGalleryActions.SELECT_MEDIA_ACTION -> {
                                onEnableSelectMode()
                            }

                            ExtendedGalleryActions.SELECT_ALL_ITEMS_ACTION -> {
                                onAllItemsSelectAction()
                            }

                            ExtendedGalleryActions.SHARE_SELECTED_ITEMS_ACTION -> {
                                onSharedItemsAction()
                            }

                            ExtendedGalleryActions.DELETE_SELECTED_ITEMS_ACTION -> {
                                onDeleteItemsAction()
                            }

                            ExtendedGalleryActions.UNLOCK_DEVICE_ACTION -> {
                                onDeviceUnlockRequest()
                            }
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                )
            },
        )
    }
}