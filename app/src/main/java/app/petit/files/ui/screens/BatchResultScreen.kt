package app.petit.files.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.petit.files.R
import app.petit.files.model.BatchCompressionResult
import app.petit.files.model.CompressorUiState
import app.petit.files.utils.formatFileSize
import java.io.File

@Composable
fun BatchResultScreen(
    state: CompressorUiState,
    onSaveResult: (String) -> Unit,
    onSaveAll: () -> Unit,
    onCompressMore: () -> Unit
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.share_video_title)
    val needsSave = state.batchResults.any { it.succeeded && !it.saveSuccess }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.batch_compression_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.batchResults, key = { it.queueId }) { result ->
                BatchResultRow(
                    result = result,
                    onShare = {
                        val uri = result.compressedUri ?: return@BatchResultRow
                        shareCompressedFile(context, uri, shareTitle)
                    },
                    onSave = { onSaveResult(result.queueId) }
                )
            }
        }

        if (needsSave) {
            FilledTonalButton(
                onClick = onSaveAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.batch_save_all), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onCompressMore,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(stringResource(R.string.compress_another_video), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BatchResultRow(
    result: BatchCompressionResult,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.succeeded) Icons.Default.Check else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (result.succeeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = result.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (result.succeeded) {
                Text(
                    text = stringResource(
                        R.string.batch_summary_sizes,
                        formatFileSize(result.originalSizeBytes),
                        formatFileSize(result.compressedSizeBytes)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.share), fontWeight = FontWeight.Bold)
                    }
                    if (!result.saveSuccess) {
                        FilledTonalButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.save_action), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    text = result.errorMessage ?: stringResource(R.string.batch_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun shareCompressedFile(context: android.content.Context, uri: Uri, chooserTitle: String) {
    try {
        val file = File(uri.path!!)
        val contentUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    } catch (_: Exception) {
    }
}
