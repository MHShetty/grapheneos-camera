package app.grapheneos.camera.ui.composable.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.grapheneos.camera.R

@Composable
fun CommonAlertDialog(
    titleText: String? = null,
    message: String? = null,
    confirmButtonText: String = stringResource(R.string.ok),
    cancelButtonText: String = stringResource(R.string.cancel),
    confirmationCallback: () -> Unit = {},
    dismissCallback: () -> Unit = {},
) = Dialog(
    content = {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 12.dp,
                        top = 20.dp,
                        bottom = 10.dp
                    )
            ) {
                if (titleText != null) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (message != null) {
                    Text(
                        modifier = Modifier.padding(
                            top = 12.dp,
                            bottom = 6.dp,
                            end = 12.dp,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        text = message
                    )
                }

                Row (
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                ) {
                    TextButton(onClick = dismissCallback) {
                        Text(cancelButtonText)
                    }

                    TextButton(onClick = {
                        confirmationCallback()
                        dismissCallback()
                    }) {
                        Text(confirmButtonText)
                    }


                }
            }
        }

    },

    onDismissRequest = dismissCallback
)