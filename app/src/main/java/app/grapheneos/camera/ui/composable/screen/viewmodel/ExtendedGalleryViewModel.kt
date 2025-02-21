package app.grapheneos.camera.ui.composable.screen.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.R
import app.grapheneos.camera.ktx.isDeviceLocked
import app.grapheneos.camera.ui.composable.model.NoDataSnackBarMessage
import app.grapheneos.camera.ui.composable.model.SnackBarMessage
import app.grapheneos.camera.util.getMimeTypeForItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

private const val TAG = "ExtendedGalleryViewModel"

class ExtendedGalleryViewModel(context: Context) : ViewModel() {

    val selectedItems = mutableStateListOf<CapturedItem>()

    var selectMode by mutableStateOf(false)
        private set

    private val capturedItemsViewModel = CapturedItemsRepository.get(context)

    private val capturedItems : SnapshotStateList<CapturedItem>
        get() = capturedItemsViewModel.capturedItems

    val groupedCapturedItems by derivedStateOf {
        capturedItems.groupBy {
            it.dateString.substringBefore('_')
        }.toSortedMap { a, b ->
            b.toInt() - a.toInt()
        }
    }

    val hasCapturedItems by derivedStateOf {
        capturedItems.isNotEmpty()
    }

    val isLoadingCapturedItems by derivedStateOf {
        capturedItemsViewModel.isLoading
    }

    val isSecureCapturedItemsLoaded by derivedStateOf {
        capturedItemsViewModel.secureModeState == true
    }

    var isDeletionDialogVisible by mutableStateOf(false)

    var snackBarMessage by mutableStateOf<SnackBarMessage>(NoDataSnackBarMessage)
        private set

    fun showSnackBar(message: String) {
        snackBarMessage = SnackBarMessage(message)
    }

    fun hideSnackBar() {
        snackBarMessage = NoDataSnackBarMessage
    }

    fun showDeletionDialog(context: Context) {
        viewModelScope.launch {
            if (selectedItems.isEmpty()) {
                showSnackBar(context.getString(R.string.select_an_item_request))
                return@launch
            }
            isDeletionDialogVisible = true
        }
    }

    fun dismissDeletionDialog() {
        viewModelScope.launch {
            isDeletionDialogVisible = false
        }
    }

    fun toggleSelection(capturedItem: CapturedItem, exitModeOnNoItemSelected : Boolean = true) {
        viewModelScope.launch {
            enterSelectionMode()
            if (hasSelected(capturedItem)) {
                deselectItem(capturedItem, exitModeOnNoItemSelected)
            } else {
                selectItem(capturedItem, true)
            }
        }
    }

    fun toggleGroupSelection(capturedItems: Collection<CapturedItem>, exitModeOnNoItemSelected : Boolean = true) {
        viewModelScope.launch {
            if (hasSelectedAll(capturedItems)) {
                deselectItems(capturedItems, exitModeOnNoItemSelected)
            } else {
                selectItems(capturedItems)
            }
        }
    }

    fun selectAllItems() {
        viewModelScope.launch {
            selectedItems.clear()
            selectedItems.addAll(capturedItems)
        }
    }

    // Ensures that the app is in selection mode
    fun enterSelectionMode() {
        viewModelScope.launch {
            selectMode = true
        }
    }

    // Ensures that the app is no longer in selection mode
    fun exitSelectionMode() {
        viewModelScope.launch {
            selectedItems.clear()
            selectMode = false
        }
    }

    fun hasSelected(capturedItem: CapturedItem): Boolean {
        return capturedItem in selectedItems
    }

    fun hasSelectedAll(capturedItems: Collection<CapturedItem>) : Boolean {
        return selectedItems.containsAll(capturedItems)
    }

    private fun selectItem(capturedItem: CapturedItem, skipCheck : Boolean = false) {
        viewModelScope.launch {
            enterSelectionMode()
            if (skipCheck || !hasSelected(capturedItem)) {
                selectedItems.add(capturedItem)
            }
        }
    }

    private fun deselectItem(capturedItem: CapturedItem, exitModeOnNoItemSelected: Boolean = true) {
        viewModelScope.launch {
            selectedItems.remove(capturedItem)

            if (exitModeOnNoItemSelected) {
                if (selectedItems.isEmpty()) {
                    exitSelectionMode()
                }
            }
        }
    }

    private fun selectItems(capturedItems: Collection<CapturedItem>) {
        viewModelScope.launch {
            for (capturedItem in capturedItems) selectItem(capturedItem)
        }
    }

    private fun deselectItems(capturedItems: Collection<CapturedItem>, exitModeOnNoItemSelected : Boolean = true) {
        viewModelScope.launch {
            for (capturedItem in capturedItems) deselectItem(capturedItem, exitModeOnNoItemSelected)
        }
    }

    fun shareSelectedItems(context: Context) {
        viewModelScope.launch {
            if (context.isDeviceLocked()) {
                showSnackBar(context.getString(R.string.sharing_not_allowed))
                return@launch
            }

            if (selectedItems.isEmpty()) {
                showSnackBar(context.getString(R.string.select_an_item_request))
                return@launch
            }

            val uris = selectedItems.mapTo(arrayListOf()) { it.uri }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    uris
                )
                type = getMimeTypeForItems(selectedItems)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.share_image)
                )
            )
        }
    }

    fun deleteSelectedItems(
        context: Context,
        onLastItemDeletion: () -> Unit = {},
    ) {
        viewModelScope.launch {

            val selectedItemsSize = selectedItems.size

            val failedDeletions = selectedItems.map { capturedItem -> async(Dispatchers.IO) {
                capturedItemsViewModel.deleteItem(capturedItem, context)
            } }.awaitAll().count { res -> !res }

            exitSelectionMode()

            if (!hasCapturedItems) {
                onLastItemDeletion()
            }

            if (failedDeletions != 0) {
                showSnackBar(context.getString(R.string.failed_multiple_deletion_message, failedDeletions, selectedItemsSize))
            }
        }
    }
}

