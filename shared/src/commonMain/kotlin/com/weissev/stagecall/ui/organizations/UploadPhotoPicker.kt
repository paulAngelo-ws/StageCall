package com.weissev.stagecall.ui.organizations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlinx.coroutines.launch

private const val MaxPostImageBytes = 5 * 1024 * 1024
private val PngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
private val JpegSignature = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())

internal data class UploadPhoto(val bytes: ByteArray, val contentType: String, val preview: ImageBitmap)

@Composable
internal fun rememberUploadPhotoPicker(
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSelected: (UploadPhoto) -> Unit,
): PickerResultLauncher {
    val scope = rememberCoroutineScope()
    return rememberFilePickerLauncher(type = PickerType.Image) { file ->
        if (file != null) scope.launch {
            onLoading(true)
            onError(null)
            try {
                require((file.getSize() ?: 0L) <= MaxPostImageBytes) { "Choose an image up to 5 MB." }
                val bytes = file.readBytes()
                require(bytes.size <= MaxPostImageBytes) { "Choose an image up to 5 MB." }
                val contentType = when {
                    bytes.take(JpegSignature.size) == JpegSignature.toList() -> "image/jpeg"
                    bytes.take(PngSignature.size) == PngSignature.toList() -> "image/png"
                    else -> error("Choose a JPEG or PNG image.")
                }
                onSelected(UploadPhoto(bytes, contentType, bytes.decodeToImageBitmap()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Could not open this image.")
            } finally {
                onLoading(false)
            }
        }
    }
}
