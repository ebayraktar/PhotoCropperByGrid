package com.bayraktar.photo_cropper.utils

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Created by emrebayraktar on 31,March,2022
 */
inline fun <reified T> Any.findGenericSuperclass(): ParameterizedType? {
    return javaClass.findGenericSuperclass(T::class.java)
}

tailrec fun <T> Type.findGenericSuperclass(targetType: Class<T>): ParameterizedType? {
    val genericSuperclass = ((this as? Class<*>)?.genericSuperclass) ?: return null
    if ((genericSuperclass as? ParameterizedType)?.rawType == targetType)
        return genericSuperclass
    return genericSuperclass.findGenericSuperclass(targetType)
}
