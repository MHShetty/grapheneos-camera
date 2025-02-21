package app.grapheneos.camera.ui.composable.component.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.IntSize

import androidx.compose.ui.unit.dp
import app.grapheneos.camera.ktx.toPx
import app.grapheneos.camera.ui.composable.component.tooltip.QuickTooltip

data class TopBarAction(
    val id : Any,
    val title: String,
    val icon: ImageVector,
    val alwaysInMoreOptions : Boolean = false
)

private const val TAG = "TopBarActions"

// 40.0.dp was taken from an constant internal to the material library
// IconButtonTokens.StateLayerSize used by IconButton's code internally
private val SPACE_PER_ICON_BUTTON = 40.dp

@Composable
fun TopBarActions(
    actions : List<TopBarAction>,
    onActionClicked: (id: Any) -> Unit,
    modifier: Modifier = Modifier,
) {

    var size by remember {
        mutableStateOf(IntSize.Zero)
    }

    var dropDownMenuExpanded by remember {
        mutableStateOf(false)
    }

    Box (
        modifier = Modifier
            .then(modifier)
            .onGloballyPositioned {
                size = it.size
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        if (size == IntSize.Zero) return@Box

        val width = size.width

        // Subtract 1 for the more options icon
        val maxVisibleActions = (width / SPACE_PER_ICON_BUTTON.toPx()).toInt() - 1

        // Ensure there is space to at least have the more options icon
        assert(maxVisibleActions >= 0) {
            "Please ensure that TopBarActions gets a width of 40.dp at least"
        }

        val visibleActions = arrayListOf<TopBarAction>()
        val moreOptionsActions = arrayListOf<TopBarAction>()

        for (action in actions) {
            if (action.alwaysInMoreOptions || visibleActions.size >= maxVisibleActions) {
                moreOptionsActions.add(action)
            } else {
                visibleActions.add(action)
            }
        }

        Row (horizontalArrangement = Arrangement.End) {
            for (visibleAction in visibleActions) {
                QuickTooltip(
                    message = visibleAction.title
                ) {
                    IconButton(onClick = {
                        onActionClicked(visibleAction.id)
                    }) {
                        Icon(visibleAction.icon, visibleAction.title)
                    }
                }

            }

            Box {
                if (moreOptionsActions.isNotEmpty()) {
                    QuickTooltip(
                        message = "More Options"
                    ) {
                        IconButton(onClick = {
                            dropDownMenuExpanded = !dropDownMenuExpanded
                        }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                "More Options"
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropDownMenuExpanded,
                        onDismissRequest = {
                            dropDownMenuExpanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth(.5f)
                            .widthIn(max = 200.dp)
                    ) {
                        for (moreOptionsAction in moreOptionsActions) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = moreOptionsAction.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                        )
                                    )
                                },

                                leadingIcon = {
                                    Icon(
                                        imageVector = moreOptionsAction.icon,
                                        contentDescription = moreOptionsAction.title,
                                    )
                                },

                                onClick = {
                                    onActionClicked(moreOptionsAction.id)
                                    dropDownMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }


    }

}