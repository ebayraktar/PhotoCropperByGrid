package com.bayraktar.photo_cropper.utils

import android.os.Environment

/**
 * Created by emrebayraktar on 31,March,2022
 */
object StorageUtils {
    fun isExternalStorageReadOnly(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
    }

    fun isExternalStorageAvailable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == extStorageState
    }
}