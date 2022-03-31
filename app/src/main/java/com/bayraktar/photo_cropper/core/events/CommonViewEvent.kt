package com.bayraktar.photo_cropper.core.events

import androidx.annotation.StringRes
import com.bayraktar.photo_cropper.core.markers.ViewEvent

/**
 * Created by emrebayraktar on 31,March,2022
 */
sealed class CommonViewEvent : ViewEvent {
    data class ShowMessage(
        val type: MessageType,
        val message: String
    ) : CommonViewEvent()

    data class ShowMessageRes(
        val type: MessageType,
        @StringRes val message: Int
    ) : CommonViewEvent()

    enum class MessageType {
        SUCCESS, ERROR
    }
}