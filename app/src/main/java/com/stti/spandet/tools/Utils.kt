package com.stti.spandet.tools

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.stti.spandet.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
private const val UIDATE_FORMAT = "dd-MM-yyyy"
private const val MAXIMAL_SIZE = 1000000

private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())

fun getImageUri(context: Context): Uri {
    var uri: Uri? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
        }
        uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }
    return uri ?: getImageUriForPreQ(context)
}

private fun getImageUriForPreQ(context: Context): Uri {
    val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File(filesDir, "/MyCamera/$timeStamp.jpg")
    if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()
    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        imageFile
    )
}

fun convertMillisToDirName(millis: Long): String {
    val formatter = SimpleDateFormat("dd-MM-yyyy--HHmmss", Locale.US)
    return formatter.format(Date(millis))
}

/** ✅ Convert millis to ISO 8601 format */
fun convertMillisToIsoTime(millis: Long): String {
    return Instant.ofEpochMilli(millis).toString() // Example: 2024-05-06T13:20:15Z
}

/** ✅ Convert ISO 8601 time string to readable format "dd-MM-yyyy HH:mm:ss" */
fun convertIsoToReadable(isoTime: String): String {
    return try {
        val instant = Instant.parse(isoTime)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "Invalid Date"
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun File.reduceFileImage(): File {
    val file = this
    val bitmap = BitmapFactory.decodeFile(file.path).getRotatedBitmap(file)
    return reduceBitmapToFile(bitmap, file)
}

/** Reduce bitmap size and save to file with proper orientation */
fun reduceBitmapToFile(bitmap: Bitmap?, file: File): File {
    // First check and correct orientation if needed
    val orientedBitmap = bitmap?.let { ensurePortraitOrientation(it) }
    
    // Then compress the image
    var compressQuality = 100
    var streamLength: Int
    do {
        val bmpStream = ByteArrayOutputStream()
        orientedBitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
        val bmpPicByteArray = bmpStream.toByteArray()
        streamLength = bmpPicByteArray.size
        compressQuality -= 5
    } while (streamLength > MAXIMAL_SIZE)
    orientedBitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(file))
    return file
}

/** Ensure image is in portrait orientation */
fun ensurePortraitOrientation(bitmap: Bitmap): Bitmap {
    // If width is greater than height, it's in landscape mode and needs rotation
    return if (bitmap.width > bitmap.height) {
        // Rotate 90 degrees to make it portrait
        val matrix = Matrix()
        matrix.postRotate(90f)
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        // Already in portrait mode, return as is
        bitmap
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Bitmap.getRotatedBitmap(file: File): Bitmap? {
    val orientation = ExifInterface(file).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
    )
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(this, 90F)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(this, 180F)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(this, 270F)
        ExifInterface.ORIENTATION_NORMAL -> this
        else -> this
    }
}

fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        source, 0, 0, source.width, source.height, matrix, true
    )
}