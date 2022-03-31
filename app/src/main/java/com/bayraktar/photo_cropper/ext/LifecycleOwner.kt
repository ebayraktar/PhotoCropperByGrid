package com.bayraktar.photo_cropper.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bayraktar.photo_cropper.utils.Event

/**
 * Created by emrebayraktar on 31,March,2022
 */

fun <T> LifecycleOwner.observe(liveData: LiveData<T>, observer: (T) -> Unit) {
    liveData.observe(this, { it?.let { t -> observer(t) } })
}

fun <T> LifecycleOwner.observe(liveData: MutableLiveData<T>, observer: (T) -> Unit) {
    liveData.observe(this, { it?.let { t -> observer(t) } })
}

fun <T> LifecycleOwner.observeEvent(liveData: LiveData<Event<T>>, observer: (T) -> Unit) {
    liveData.observe(this, { it?.getContentIfNotHandled()?.let { t -> observer(t) } })
}