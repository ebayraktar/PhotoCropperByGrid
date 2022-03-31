package com.bayraktar.photo_cropper.ui

import com.bayraktar.photo_cropper.core.markers.ViewEvent

/**
 * Created by emrebayraktar on 31,March,2022
 */
sealed class MainViewEvent : ViewEvent {
    object ChooseImage : MainViewEvent()
    object CheckPermission : MainViewEvent()
    object WriteImage : MainViewEvent()
    data class Grid(val is3x3: Boolean) : MainViewEvent()
}