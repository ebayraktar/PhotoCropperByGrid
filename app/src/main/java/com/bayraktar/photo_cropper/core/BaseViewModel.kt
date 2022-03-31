package com.bayraktar.photo_cropper.core

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bayraktar.photo_cropper.core.events.CommonViewEvent
import com.bayraktar.photo_cropper.core.markers.ViewEvent
import com.bayraktar.photo_cropper.utils.Event

/**
 * Created by emrebayraktar on 31,March,2022
 */
abstract class BaseViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _viewEvent = MutableLiveData<Event<ViewEvent>>()
    val viewEvent: LiveData<Event<ViewEvent>> = _viewEvent

    protected fun sendViewEvent(event: ViewEvent) = _viewEvent.postValue(Event(event))

    fun showLoading() = _loading.postValue(true)
    fun hideLoading() = _loading.postValue(false)

    fun showSuccess(message: String) = showMessage(CommonViewEvent.MessageType.SUCCESS, message)
    fun showError(message: String) = showMessage(CommonViewEvent.MessageType.ERROR, message)

    fun showMessage(type: CommonViewEvent.MessageType, message: String) {
        sendViewEvent(CommonViewEvent.ShowMessage(type, message))
    }

    fun showMessage(type: CommonViewEvent.MessageType, @StringRes message: Int) {
        sendViewEvent(CommonViewEvent.ShowMessageRes(type, message))
    }
}