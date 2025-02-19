package app.grapheneos.camera.ui.composable.screen.viewmodel

import android.content.Context
import android.util.Log

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


import app.grapheneos.camera.CapturedItem
import app.grapheneos.camera.CapturedItems
import app.grapheneos.camera.ITEM_TYPE_VIDEO
import app.grapheneos.camera.ktx.isDeviceLocked
import app.grapheneos.camera.ui.activities.InAppGallery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CapturedItemsRepository"

class CapturedItemsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val showVideosOnly: Boolean = false,
    private val placeholderItem : CapturedItem? = null,
    private val mediaItems: ArrayList<CapturedItem>? = null,
    preload : Boolean = true,
) {
    var isLoading by mutableStateOf(false)

    val capturedItems = mutableStateListOf<CapturedItem>()

    var secureModeState by mutableStateOf<Boolean?>(null)
        private set

    init {
        if (preload) loadCapturedItems()
    }

    fun loadCapturedItems(
        onLoadComplete : () -> Unit = {}
    ) {
        // Only reload on config (i.e. secure mode states) changes
        val secureModeState = context.isDeviceLocked()
        if (this.secureModeState == secureModeState) {
            onLoadComplete()
            return
        }
        this.secureModeState = secureModeState

        scope.launch {
            Log.i(TAG, "loadCapturedItems is loading items 1")
            if (isLoading) return@launch
            isLoading = true

            Log.i(TAG, "loadCapturedItems is loading items 2")

            capturedItems.clear()

            Log.i(TAG, "loadCapturedItems is loading items 3")

            if (placeholderItem != null) {
                capturedItems.add(placeholderItem)
            }

            Log.i(TAG, "loadCapturedItems is loading items 4")

            val items : List<CapturedItem>

            Log.i(TAG, "isSecureMode: $secureModeState")

            withContext(Dispatchers.IO) {
                items = if (secureModeState) {
                    mediaItems ?: arrayListOf()
                } else {
                    CapturedItems.get(context)
                }
            }

            Log.i(TAG, "items 1: $items")

            val relevantMediaItems = if (showVideosOnly) {
                items.filter { capturedItem -> capturedItem.type == ITEM_TYPE_VIDEO }
            } else {
                items
            }

            Log.i(TAG, "items 2: $items")

            val finalItems = relevantMediaItems.sortedByDescending { it.dateString }

            Log.i(TAG, "finalItems: $items")



            capturedItems.clear()
            capturedItems.addAll(finalItems)

            isLoading = false

            onLoadComplete()
        }
    }

    suspend fun deleteItem(capturedItem: CapturedItem, context: Context) : Boolean {
        val res = capturedItem.delete(context)

        if (res) {
            // On main thread to ensure sequential deletion to avoid concurrent access
            // exception when a lot of multiple items are being deleted together
            withContext(Dispatchers.Main) {
                capturedItems.remove(capturedItem)
            }
        }

        return res
    }

    companion object {
        fun get(context: Context) : CapturedItemsRepository {
            assert(context is InAppGallery) {
                "CapturedItemsRepository only support instantiating from InAppGallery activity currently"
            }

            return (context as InAppGallery).capturedItemsRepository
        }
    }
}