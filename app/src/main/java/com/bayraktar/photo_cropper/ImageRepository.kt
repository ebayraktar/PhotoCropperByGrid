package com.bayraktar.photo_cropper

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


/**
 * Created by Emre BAYRAKTAR on 5/15/2021.
 */

class ImageRepository(private val context: Context) {

    fun save(bitmapList: List<Bitmap>) {

        val observable = Observable.just(bitmapList)

        val observer = object : Observer<List<Bitmap>> {
            override fun onSubscribe(d: Disposable?) {
                //onSubscribe
            }

            override fun onNext(bitmaps: List<Bitmap>) {

                for (bitmap in bitmaps) {
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            System.currentTimeMillis().toString()
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES + File.separator + "PhotoCropper"
                            )
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                }

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    for (element in bitmaps)
//                        saveImageInQ(element)
//                } else {
//                    for (element in bitmaps)
//                        saveTheImageLegacyStyle(element)
//                }
            }

            override fun onError(e: Throwable?) {
                AlertDialog.Builder(context)
                    .setMessage("Hata: " + e?.message)
                    .setPositiveButton("TAMAM", null)
                    .create().show()
            }

            override fun onComplete() {
                AlertDialog.Builder(context)
                    .setMessage("İşlem başarılı.")
                    .setPositiveButton("TAMAM", null)
                    .create().show()
            }
        }

        observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                Log.d("TAG", "save: " + it.message)
            }
            .subscribe(observer)

    }

    //Make sure to call this function on a worker thread, else it will block main thread
    private fun saveTheImageLegacyStyle(bitmap: Bitmap) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val folderName = File(Environment.DIRECTORY_DCIM)

        val file = File(
            folderName, filename
        )

        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        out.flush()
        out.close()
    }

    //Make sure to call this function on a worker thread, else it will block main thread
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInQ(bitmap: Bitmap) {

        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream?
        var imageUri: Uri?
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + "PhotoCropper"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = context.contentResolver

        contentResolver.also { resolver ->
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }

        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        imageUri?.let { contentResolver.update(it, contentValues, null, null) }
    }

}