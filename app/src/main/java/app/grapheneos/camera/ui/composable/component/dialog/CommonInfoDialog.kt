package app.grapheneos.camera.ui.composable.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.grapheneos.camera.R

@Composable
fun CommonInfoDialog(
    titleText: String? = null,
    message: String? = null,
    dismissText: String = stringResource(R.string.ok),
    dismissCallback: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = dismissCallback,

        content = {
            Surface(
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            start = 24.dp,
                            top = 20.dp,
                            bottom = 8.dp
                        )
                ) {
                    if (titleText != null) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(bottom = 20.dp)
                        )
                    }

                    if (message != null) {
                        Row (
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = message
                            )

                        }
                    }



                    TextButton(
                        onClick = dismissCallback,
                        modifier = Modifier
                            .align(alignment = Alignment.End)
                            .padding(end = 12.dp)
                    ) {
                        Text(text = dismissText)
                    }

                }
            }

        }
    )
}