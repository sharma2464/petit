package app.petit.files.model

import android.net.Uri

data class QueuedVideo(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val fileTypeLabel: String,
    val sizeBytes: Long
)
