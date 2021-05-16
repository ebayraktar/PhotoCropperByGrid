package com.bayraktar.photo_cropper

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Created by Emre BAYRAKTAR on 5/15/2021.
 */

private val PROJECTION = arrayOf(MediaStore.Images.Media._ID)
private const val QUERY = MediaStore.Images.Media.DISPLAY_NAME + " = ?"

class ImageRepository(private val context: Context) {
    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        ) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI


    suspend fun getLocalUri(filename: String): Uri? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver

            resolver.query(collection, PROJECTION, QUERY, arrayOf(filename), null)
                ?.use { cursor ->
                    if (cursor.count > 0) {
                        cursor.moveToFirst()
                        return@withContext ContentUris.withAppendedId(
                            collection,
                            cursor.getLong(0)
                        )
                    }
                }

            null
        }

    fun save(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveImageInQ(bitmap)
        else saveTheImageLegacyStyle(bitmap)
    }

    //Make sure to call this function on a worker thread, else it will block main thread
    private fun saveTheImageLegacyStyle(bitmap: Bitmap) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        val image = File(imagesDir, filename)
        val fos = FileOutputStream(image)
        fos.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
        }
        fos.close()
    }

    //Make sure to call this function on a worker thread, else it will block main thread
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInQ(bitmap: Bitmap): Uri {

        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream?
        var imageUri: Uri?
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/PhotoCropper"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = context.contentResolver

        contentResolver.also { resolver ->
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }

        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }

        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        imageUri?.let { contentResolver.update(it, contentValues, null, null) }

        return imageUri!!

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveQ(): Uri = withContext(Dispatchers.IO) {

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, System.currentTimeMillis().toString())
            put(MediaStore.Video.Media.RELATIVE_PATH, "Images/PhotoCropper")
            put(MediaStore.Video.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)

        uri?.let {
//            val sink = Okio.
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } ?: throw RuntimeException("MediaStore failed for some reason")

        uri
    }


}