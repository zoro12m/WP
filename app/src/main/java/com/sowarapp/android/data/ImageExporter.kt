package com.sowarapp.android.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.sowarapp.android.data.model.SowarImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Turns a [SowarImage] into a real JPEG file (background + Arabic caption)
 * and either saves it to the gallery or prepares it for sharing.
 *
 * The caption is drawn with Android's own text engine, so Arabic letters
 * are joined and ordered correctly.
 */
class ImageExporter(private val context: Context) {

    /** Saves the image to Pictures/SowarApp so it shows up in the phone's gallery. */
    suspend fun saveToGallery(image: SowarImage) = withContext(Dispatchers.IO) {
        val bitmap = renderImage(image)
        val fileName = "sowar_${image.id}_${System.currentTimeMillis()}.jpg"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(bitmap, fileName)
            } else {
                saveToPublicFolder(bitmap, fileName)
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Writes the image to the app's cache and returns a Uri other apps are allowed to read. */
    suspend fun createShareableUri(image: SowarImage): Uri = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        val file = File(folder, "sowar_${image.id}.jpg")
        val bitmap = renderImage(image)
        try {
            FileOutputStream(file).use { writeJpeg(bitmap, it) }
        } finally {
            bitmap.recycle()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun renderImage(image: SowarImage): Bitmap {
        val options = BitmapFactory.Options().apply { inMutable = true }
        val bitmap = BitmapFactory.decodeResource(context.resources, image.imageRes, options)
            ?: error("Could not decode image ${image.id}")
        val canvas = Canvas(bitmap)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            // Same proportions as CaptionedImage in the UI, so the file looks like the screen.
            textSize = bitmap.width * CAPTION_SIZE_RATIO
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(bitmap.width * 0.015f, 0f, bitmap.width * 0.005f, Color.argb(160, 0, 0, 0))
        }
        val textWidth = (bitmap.width * CAPTION_WIDTH_RATIO).toInt()
        val layout = StaticLayout.Builder
            .obtain(image.caption, 0, image.caption.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(
            (bitmap.width - textWidth) / 2f,
            bitmap.height * CAPTION_VERTICAL_POSITION - layout.height / 2f,
        )
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(bitmap: Bitmap, fileName: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
            // Hide the file from other apps until it is fully written.
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused to create the file")
        try {
            val stream = resolver.openOutputStream(uri) ?: error("Could not open the new file")
            stream.use { writeJpeg(bitmap, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    /** Android 9 and older: write directly to the Pictures folder (needs the storage permission). */
    @Suppress("DEPRECATION")
    private fun saveToPublicFolder(bitmap: Bitmap, fileName: String) {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            ALBUM_NAME,
        )
        if (!folder.exists() && !folder.mkdirs()) error("Could not create $folder")
        val file = File(folder, fileName)
        FileOutputStream(file).use { writeJpeg(bitmap, it) }
        // Tell the gallery app that a new picture exists.
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(MIME_TYPE), null)
    }

    private fun writeJpeg(bitmap: Bitmap, output: OutputStream) {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
            error("Could not encode the image")
        }
    }

    companion object {
        const val CAPTION_SIZE_RATIO = 0.085f
        const val CAPTION_WIDTH_RATIO = 0.84f
        const val CAPTION_VERTICAL_POSITION = 0.6f

        private const val ALBUM_NAME = "SowarApp"
        private const val SHARE_FOLDER = "shared_images"
        private const val MIME_TYPE = "image/jpeg"
        private const val JPEG_QUALITY = 92
    }
}
