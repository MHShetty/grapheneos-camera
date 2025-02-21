package app.grapheneos.camera.ui.composable

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.ktx.popBackStack
import app.grapheneos.camera.ui.composable.screen.ui.ExtendedGalleryScreen
import app.grapheneos.camera.ui.composable.model.VideoUri
import app.grapheneos.camera.ui.composable.screen.routes.ExtendedGalleryRoute

import app.grapheneos.camera.ui.composable.screen.routes.GalleryRoute
import app.grapheneos.camera.ui.composable.screen.routes.VideoPlayerRoute

import app.grapheneos.camera.ui.composable.screen.ui.GalleryScreen
import app.grapheneos.camera.ui.composable.screen.ui.VideoPlayerScreen
import app.grapheneos.camera.ui.composable.theme.appColorScheme
import app.grapheneos.camera.ui.composable.theme.appTypography

@Composable
fun CameraApp(
    initialRoute: Any,
    onExitAction : () -> Unit,
) {
    val navController = rememberNavController()

    val defaultBackAction = {
        // Go to previous screen if present, else perform exit action
        navController.popBackStack(onBackStackEmpty = onExitAction)
    }

    MaterialTheme(
        colorScheme = appColorScheme(),
        typography = appTypography(),
    ) {
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            }
        ) {
            composable<GalleryRoute>(typeMap = GalleryRoute.typeMap) { backStackEntry ->

                val args = backStackEntry.toRoute<GalleryRoute>()

                GalleryScreen(
                    focusItem = args.focusItem,
                    showVideoPlayerAction = { capturedItem ->
                        navController.navigate(
                            VideoPlayerRoute(videoUri = VideoUri(capturedItem.uri))
                        )
                    },
                    showExtendedGalleryAction = {
                        navController.navigate(ExtendedGalleryRoute)
                    },
                    onExitAction = defaultBackAction,
                )
            }

            composable<ExtendedGalleryRoute> {
                ExtendedGalleryScreen(
                    showMediaItemAction = { capturedItem ->
                        if (capturedItem.type == ITEM_TYPE_VIDEO) {
                            navController.navigate(
                                VideoPlayerRoute(
                                    videoUri = VideoUri(capturedItem.uri)
                                )
                            )
                        } else {
                            navController.navigate(
                                GalleryRoute(
                                    focusItem = capturedItem
                                )
                            )
                        }

                    },
                    onExitAction = defaultBackAction
                )
            }

            composable<VideoPlayerRoute>(typeMap = VideoPlayerRoute.typeMap) {
                val args = it.toRoute<VideoPlayerRoute>()

                VideoPlayerScreen(
                    mediaUri = args.videoUri.uri,
                    onExitAction = defaultBackAction,
                )
            }
        }
    }

}