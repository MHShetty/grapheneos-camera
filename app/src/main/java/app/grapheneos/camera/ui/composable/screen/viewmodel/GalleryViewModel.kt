package app.grapheneos.camera.ui.composable.screen.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.R
import app.grapheneos.camera.ktx.isDeviceLocked
import app.grapheneos.camera.ui.composable.model.MediaItemDetails
import app.grapheneos.camera.ui.composable.model.NoDataSnackBarMessage
import app.grapheneos.camera.ui.composable.model.SnackBarMessage
import kotlinx.coroutines.launch

private const val TAG = "GalleryViewModel"

class GalleryViewModel(context: Context) : ViewModel() {

    var focusItem by mutableStateOf<CapturedItem?>(null)

    var isZoomedIn by mutableStateOf(false)

    var inFocusMode by mutableStateOf(false)

    var displayedMediaItem by mutableStateOf<MediaItemDetails?>(null)

    var deletionItem by mutableStateOf<CapturedItem?>(null)

    var currentPage = 0

    private val currentItem: CapturedItem
        get() = capturedItems[currentPage]

    private val capturedItemsViewModel = CapturedItemsRepository.get(context)

    val capturedItems : SnapshotStateList<CapturedItem>
        get() = capturedItemsViewModel.capturedItems

    val hasCapturedItems: Boolean
        get() = capturedItemsViewModel.capturedItems.isNotEmpty()

    val isLoadingCapturedItems by derivedStateOf {
        capturedItemsViewModel.isLoading
    }

    var snackBarMessage by mutableStateOf<SnackBarMessage>(NoDataSnackBarMessage)
        private set

    fun showSnackBar(message: String) {
        snackBarMessage = SnackBarMessage(message)
    }

    fun hideSnackBar() {
        snackBarMessage = NoDataSnackBarMessage
    }

    fun displayMediaInfo(context: Context, item: CapturedItem = currentItem) {
        viewModelScope.launch {
            if (!hasCapturedItems) {
                showSnackBar(
                    context.getString(R.string.unable_to_obtain_file_details)
                )
                return@launch
            }

            try {
                displayedMediaItem = MediaItemDetails.forCapturedItem(context, item)
            } catch (e: Exception) {
                Log.i(TAG, "Unable to obtain file details for MediaInfoDialog")
                e.printStackTrace()
                showSnackBar(
                    context.getString(R.string.unable_to_obtain_file_details)
                )
            }

        }

    }

    fun hideMediaInfoDialog() {
        viewModelScope.launch {
            displayedMediaItem = null
        }
    }

    fun promptItemDeletion(item: CapturedItem = currentItem) {
        viewModelScope.launch {
            deletionItem = item
        }
    }


    fun deleteMediaItem(
        context: Context,
        item: CapturedItem,
        onLastItemDeletion: () -> Unit,
    ) {
        viewModelScope.launch {
            val result = capturedItemsViewModel.deleteItem(item, context)

            if (result) {
                if (!hasCapturedItems) {
                    Toast.makeText(context, R.string.empty_gallery, Toast.LENGTH_LONG)
                        .show()
                    onLastItemDeletion()
                } else {
                    showSnackBar(
                        context.getString(R.string.deleted_successfully)
                    )
                }
            } else {
                showSnackBar(
                    context.getString(R.string.deleting_unexpected_error)
                )
            }
        }
    }

    fun hideDeletionPrompt() {
        viewModelScope.launch {
            deletionItem = null
        }
    }

    fun editMediaItem(
        context: Context,
        chooseApp: Boolean = false,
        modifyOriginal: Boolean = false,
        item: CapturedItem = currentItem,
    ) {
        viewModelScope.launch {
            if (context.isDeviceLocked()) {
                showSnackBar(
                    context.getString(R.string.edit_not_allowed)
                )
                return@launch
            }

            val editIntent = Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(item.uri, item.mimeType())
                putExtra(Intent.EXTRA_STREAM, item.uri)
                if (modifyOriginal) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } else {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            if (chooseApp) {
                val chooser = Intent.createChooser(
                    editIntent,
                    context.getString(R.string.edit_image)
                ).apply {
                    putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false)
                }
                context.startActivity(chooser)
            } else {
                try {
                    context.startActivity(editIntent)
                } catch (ignored: ActivityNotFoundException) {
                    showSnackBar(
                        context.getString(R.string.no_editor_app_error)
                    )
                }
            }
        }
    }

    fun shareCurrentItem(
        context: Context,
        item: CapturedItem = currentItem,
    ) {
        viewModelScope.launch {
            if (context.isDeviceLocked()) {
                showSnackBar(
                    context.getString(R.string.sharing_not_allowed)
                )
                return@launch
            }

            val share = Intent(Intent.ACTION_SEND)
            share.putExtra(Intent.EXTRA_STREAM, item.uri)
            share.setDataAndType(item.uri, item.mimeType())
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            context.startActivity(
                Intent.createChooser(share, context.getString(R.string.share_image))
            )
        }
    }

    fun updateZoomedState(zoomedIn: Boolean) {
        viewModelScope.launch {
            isZoomedIn = zoomedIn
            inFocusMode = zoomedIn
        }
    }

    fun toggleFocusMode() {
        viewModelScope.launch {
            inFocusMode = !inFocusMode
        }
    }
}