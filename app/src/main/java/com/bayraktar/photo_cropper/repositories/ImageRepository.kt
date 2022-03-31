package com.bayraktar.photo_cropper.repositories

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.OutputStream

/**
 * Created by Emre BAYRAKTAR on 5/15/2021.
 */

class ImageRepository(private val context: Context) {

    fun save(
        bitmapList: List<Bitmap>,
        onError: ((Throwable?) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        val observer = object : Observer<List<Bitmap>> {
            override fun onSubscribe(d: Disposable?) {
                //onSubscribe
            }

            override fun onNext(bitmaps: List<Bitmap>) {
                for (bitmap in bitmaps) {
                    saveTheImage(bitmap)
                }
            }

            override fun onError(e: Throwable?) {
                Log.e("ImageRepository", "onError: ${e.toString()}")
            }

            override fun onComplete() {
                Log.i("ImageRepository", "onComplete: ")
            }
        }

        Observable.just(bitmapList)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete(onComplete)
            .doOnError(onError)
            .subscribe(observer)
    }

    //Make sure to call this function on a worker thread, else it will block main thread
    private fun saveTheImage(bitmap: Bitmap) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream?
        var imageUri: Uri?
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + "PhotoCropper"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        //use application context to get contentResolver
        val contentResolver = context.contentResolver
        contentResolver.also { resolver ->
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        }
        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        fos?.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            imageUri?.let { contentResolver.update(it, contentValues, null, null) }
        }
    }

}