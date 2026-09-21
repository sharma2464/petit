package compress.joshattic.us.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import org.mp4parser.Box
import org.mp4parser.IsoFile
import org.mp4parser.boxes.iso14496.part12.UserDataBox
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class SourceMediaMetadata(
    val dateString: String? = null,
    val locationIso6709: String? = null,
    val title: String? = null,
    val year: String? = null,
    val captureTimeMs: Long? = null
)

fun extractRetrieverMetadata(context: Context, uri: Uri): SourceMediaMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
        val captureTimeMs = parseCaptureTimeMs(dateString, year)
        SourceMediaMetadata(
            dateString = dateString,
            locationIso6709 = location,
            title = title,
            year = year,
            captureTimeMs = captureTimeMs
        )
    } catch (_: Exception) {
        SourceMediaMetadata()
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

private fun parseCaptureTimeMs(dateString: String?, year: String?): Long? {
    if (!dateString.isNullOrBlank()) {
        val patterns = listOf(
            "yyyyMMdd'T'HHmmss.SSSZ",
            "yyyyMMdd'T'HHmmssZ",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy:MM:dd HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                return format.parse(dateString)?.time
            } catch (_: Exception) {
            }
        }
    }
    if (!year.isNullOrBlank()) {
        return year.toIntOrNull()?.let { y ->
            SimpleDateFormat("yyyy", Locale.US).parse(y.toString())?.time
        }
    }
    return null
}

fun mergeMp4MetadataFromSource(source: File, destination: File): Boolean {
    if (!source.exists() || !destination.exists()) return false
    val temp = File(destination.parentFile, "${destination.name}.metadata.tmp")
    try {
        val sourceBoxes = IsoFile(source).use { sourceIso ->
            sourceIso.movieBox?.getBoxes(UserDataBox::class.java)?.toList() ?: emptyList()
        }
        if (sourceBoxes.isEmpty()) return true

        IsoFile(destination).use { destIso ->
            val destMovie = destIso.movieBox ?: return false
            val kept = destMovie.getBoxes(Box::class.java).filterNot { it is UserDataBox }
            destMovie.setBoxes(kept + sourceBoxes)
            FileOutputStream(temp).channel.use { channel ->
                destIso.writeContainer(channel)
            }
        }
        if (!destination.delete() || !temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        if (temp.exists()) temp.delete()
        return false
    }
}

fun mergeMp4MetadataFromUri(context: Context, sourceUri: Uri, destination: File): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            val tempSource = File.createTempFile("meta_src_", ".mp4", context.cacheDir)
            try {
                FileOutputStream(tempSource).use { out ->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                        input.copyTo(out)
                    }
                }
                mergeMp4MetadataFromSource(tempSource, destination)
            } finally {
                tempSource.delete()
            }
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
