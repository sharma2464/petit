package compress.joshattic.us.model

import android.net.Uri

data class BatchCompressionResult(
    val queueId: String,
    val displayName: String,
    val originalSizeBytes: Long,
    val compressedUri: Uri? = null,
    val compressedSizeBytes: Long = 0L,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val captureTimeMs: Long? = null
) {
    val succeeded: Boolean get() = errorMessage == null && compressedUri != null
}

data class BatchProgress(
    val currentIndex: Int,
    val total: Int,
    val currentName: String
)
