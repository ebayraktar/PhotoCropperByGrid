package com.bayraktar.photo_cropper.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bayraktar.photo_cropper.core.BaseViewModel
import com.bayraktar.photo_cropper.repositories.ImageRepository
import com.bayraktar.photo_cropper.utils.ImageUtils
import com.bayraktar.photo_cropper.utils.StorageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by emrebayraktar on 31,March,2022
 */

@HiltViewModel
class MainViewModel @Inject constructor(private val imageRepository: ImageRepository) :
    BaseViewModel() {

    val hasImage = MutableLiveData<Boolean>()

    fun crop() {
        if (!StorageUtils.isExternalStorageAvailable() ||
            StorageUtils.isExternalStorageReadOnly()
        ) {
            showError("Kayıt yeri bulunamadı")
            return
        }

        if (hasImage.value != true) {
            sendViewEvent(MainViewEvent.ChooseImage)
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            sendViewEvent(MainViewEvent.CheckPermission)
        else
            sendViewEvent(MainViewEvent.WriteImage)
    }

    fun chooseImage() = sendViewEvent(MainViewEvent.ChooseImage)

    fun addGrid(is3x3: Boolean) = sendViewEvent(MainViewEvent.Grid(is3x3))

    fun writeImageToFile(bitmap: Bitmap, is3x3: Boolean) {
        viewModelScope.launch {
            showLoading()
            val bitmapList = ImageUtils.cropImageByGrid(bitmap, is3x3)
            imageRepository.save(bitmapList,
                onError = {
                    hideLoading()
                    showError("Hata: " + it?.message)
                },
                onComplete = {
                    hideLoading()
                    showSuccess("İşlem başarılı")
                }
            )
        }
    }
}