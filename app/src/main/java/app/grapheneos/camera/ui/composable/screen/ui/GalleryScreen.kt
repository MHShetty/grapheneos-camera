package app.grapheneos.camera.ui.composable.screen.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.lifecycle.viewmodel.compose.viewModel
import app.grapheneos.camera.CapturedItem

import app.grapheneos.camera.ui.composable.component.dialog.FileDeletionDialog
import app.grapheneos.camera.ui.composable.component.mediapreview.MediaPreview
import app.grapheneos.camera.ui.composable.component.topbar.gallery.GalleryTopBar
import app.grapheneos.camera.ui.composable.component.dialog.MediaInfoDialog
import app.grapheneos.camera.ui.composable.theme.AppColor

import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

import app.grapheneos.camera.ITEM_TYPE_IMAGE
import app.grapheneos.camera.R
import app.grapheneos.camera.ui.composable.component.SnackBarMessageHandler
import app.grapheneos.camera.ui.composable.component.tooltip.QuickTooltip
import app.grapheneos.camera.ui.composable.component.tooltip.QuickTooltipVerticalDirection

import app.grapheneos.camera.ui.composable.screen.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

private const val TAG = "GalleryScreen"

@Composable
fun GalleryScreen(
    focusItem: CapturedItem? = null,
    showVideoPlayerAction: (CapturedItem) -> Unit = {},
    showExtendedGalleryAction: () -> Unit = {},
    onExitAction: () -> Unit = {},
) {
    val context = LocalContext.current

    val window = (context as Activity).window

    val insetsController = WindowCompat.getInsetsController(window, LocalView.current).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }

    val zoomableState = rememberZoomableState()

    val coroutineScope = rememberCoroutineScope()

    val snackBarHostState = remember {
        SnackbarHostState()
    }

    val viewModel = viewModel {
        GalleryViewModel(context)
    }

    val pagerState = rememberPagerState {
        viewModel.capturedItems.size
    }

    SnackBarMessageHandler(
        snackBarHostState = snackBarHostState,
        snackBarMessage = viewModel.snackBarMessage
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.hideSnackBar()
        }
    }

    val backgroundColor by animateColorAsState(
        label = "background_color_animation",
        targetValue = if (viewModel.inFocusMode) Color.Black else AppColor.BackgroundColor,
        animationSpec = tween(durationMillis = 300, easing = EaseIn),
    )

    // Ensure the focus is back to the focused item whenever capturedItems is updated
    LaunchedEffect(viewModel.isLoadingCapturedItems, viewModel.capturedItems) {
        if (viewModel.isLoadingCapturedItems) return@LaunchedEffect

        if (!viewModel.hasCapturedItems) {
            Toast.makeText(context, R.string.empty_gallery, Toast.LENGTH_LONG).show()
            onExitAction()
        }

        val focusIndex = viewModel.capturedItems.indexOf(focusItem)
        if (focusIndex != -1) {
            pagerState.scrollToPage(focusIndex)
        }
    }

    // Set the default/updated focus item (updated on load)
    LaunchedEffect(focusItem) {
        viewModel.focusItem = focusItem
    }

    // Update the current focus item when the user slides between pages
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        viewModel.currentPage = page
        if (page < viewModel.capturedItems.size) {
            viewModel.focusItem = viewModel.capturedItems[page]
        }

    }

    // Update zoom and focus state based on latest zoom state
    LaunchedEffect(zoomableState.zoomFraction) {
        zoomableState.zoomFraction?.let { zoomFraction ->
            val isZoomedIn = zoomFraction >= 0.01f
            viewModel.updateZoomedState(isZoomedIn)
        }
    }

    LaunchedEffect(viewModel.inFocusMode) {
        if (viewModel.inFocusMode) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Displays media info dialog when displayedMediaItem is not null
    MediaInfoDialog(
        mediaItemDetails = viewModel.displayedMediaItem,
        dismissCallback = viewModel::hideMediaInfoDialog,
    )

    // Displays item deletion dialog when deletionItem is not null
    FileDeletionDialog(
        deletionItem = viewModel.deletionItem,
        onDeleteAction = { item ->
            viewModel.deleteMediaItem(context, item, onLastItemDeletion = onExitAction)
        },
        dismissHandler = viewModel::hideDeletionPrompt,
    )

    Scaffold(
        containerColor = backgroundColor,

        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },

        floatingActionButton = {
            if (focusItem == null) {
                AnimatedVisibility(
                    visible = !viewModel.inFocusMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    QuickTooltip(
                        message = stringResource(R.string.grid_view),
                        defaultDirection = QuickTooltipVerticalDirection.TOP
                    ) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (zoomableState.zoomFraction != 0f) {
                                        zoomableState.resetZoom()
                                    }
                                    showExtendedGalleryAction()
                                }
                            },
                            shape = CircleShape,
                        ) {
                            Icon(
                                Icons.Default.GridOn,
                                contentDescription = stringResource(R.string.search_images),
                            )
                        }
                    }
                }
            }
        },

        topBar = {
            GalleryTopBar(
                visible = !viewModel.inFocusMode,
                onCloseAction = onExitAction,

                onEditAction = { chooseApp, modifyOriginal ->
                    viewModel.editMediaItem(context, chooseApp, modifyOriginal)
                },

                onDeleteAction = {
                    viewModel.promptItemDeletion()
                },

                onInfoAction = {
                    viewModel.displayMediaInfo(context)
                },

                onShareAction = {
                    viewModel.shareCurrentItem(context)
                },
            )
        },

        content = { innerPadding ->

            if (viewModel.isLoadingCapturedItems) {
                Text(
                    text = stringResource(R.string.three_dots),
                    color = Color.White,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentHeight(align = Alignment.CenterVertically),
                )
            } else {
                if (viewModel.hasCapturedItems) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !viewModel.isZoomedIn,
                        beyondViewportPageCount = 1,
                        modifier = Modifier
                            .padding(
                                innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                // the fixed padding of 56.dp has been added to GalleryImage to
                                // avoid having a fixed black bar on the top (56dp comes from
                                // material guidelines)
                                0.dp,
                                innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                                0.dp,
                            )
                            .fillMaxSize()
                    ) { page ->
                        val capturedItem = viewModel.capturedItems[page]

                        val modifier: Modifier = if (capturedItem.type == ITEM_TYPE_IMAGE) {
                            Modifier
                                .zoomable(
                                    zoomableState,
                                    onClick = {
                                        viewModel.toggleFocusMode()
                                    }
                                )
                        } else {
                            Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    showVideoPlayerAction(capturedItem)
                                }
                        }

                        MediaPreview(
                            capturedItem = capturedItem,
                            modifier = modifier
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.empty_gallery),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentHeight(align = Alignment.CenterVertically),
                    )
                }
            }
        },
    )
}