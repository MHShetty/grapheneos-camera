package app.grapheneos.camera.ui.composable.model

import java.util.UUID

data class SnackBarMessage(
    val message: String,
    val id: UUID  = UUID.randomUUID()
)

val NoDataSnackBarMessage = SnackBarMessage("")
