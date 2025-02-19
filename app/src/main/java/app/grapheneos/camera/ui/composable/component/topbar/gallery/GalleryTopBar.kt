package app.grapheneos.camera.ui.composable.component.topbar.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.grapheneos.camera.R

import app.grapheneos.camera.ui.composable.component.topbar.TopBarActions
import app.grapheneos.camera.ui.composable.theme.AppColor

private const val TAG = "GalleryTopBar"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopBar(
    visible: Boolean,
    onCloseAction: () -> Unit,
    onEditAction: (chooseApp: Boolean, modifyOriginal: Boolean) -> Unit,
    onDeleteAction: () -> Unit,
    onInfoAction: () -> Unit,
    onShareAction: () -> Unit
) {

    AnimatedVisibility(
        visible = visible,

        enter = slideInVertically(
            initialOffsetY = { height -> -height },
            animationSpec = tween(durationMillis = 300, easing = EaseIn),
        ),
        exit = slideOutVertically(
            targetOffsetY = { height -> -height },
            animationSpec = tween(durationMillis = 300, easing = EaseIn),
        ),
    ) {
        TopAppBar(
            title = {},

            colors = TopAppBarColors(
                containerColor = AppColor.AppBarColor,
                scrolledContainerColor = AppColor.AppBarColor,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),

            navigationIcon = {
                IconButton(onClick = onCloseAction) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },

            actions = {
                TopBarActions(
                    actions = galleryTopBarActions(),
                    onActionClicked = { action ->
                        when (action) {
                            GalleryActions.EDIT_MEDIA -> {
                                onEditAction(false, false)
                            }
                            GalleryActions.DELETE_MEDIA -> {
                                onDeleteAction()
                            }
                            GalleryActions.SHOW_MEDIA_INFO -> {
                                onInfoAction()
                            }
                            GalleryActions.SHARE_MEDIA -> {
                                onShareAction()
                            }
                            GalleryActions.EDIT_MEDIA_WITH_APP -> {
                                onEditAction(true, false)
                            }
                            GalleryActions.EDIT_MEDIA_IN_PLACE -> {
                                onEditAction(true, true)
                            }
                        }
                    },
                    // To avoid overlapping leading back arrow and some spacing
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                )
            },
        )
    }
}