package com.bayraktar.photo_cropper.utils

import android.graphics.Bitmap

/**
 * Created by emrebayraktar on 31,March,2022
 */
object ImageUtils {
    fun cropImageByGrid(bitmap: Bitmap, is3x3: Boolean): List<Bitmap> {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapList: ArrayList<Bitmap> = ArrayList()
        val horizontal = 3
        val vertical = if (is3x3) 3 else 2
        val freqWidth = width / horizontal
        val freqHeight = height / vertical
        for (i in 0 until horizontal) {
            for (j in 0 until vertical) {
                val startX = freqWidth * i
                val startY = freqHeight * j
                val tempBitmap = Bitmap.createBitmap(bitmap, startX, startY, freqWidth, freqHeight)
                bitmapList.add(tempBitmap)
            }
        }

        return bitmapList
    }
}