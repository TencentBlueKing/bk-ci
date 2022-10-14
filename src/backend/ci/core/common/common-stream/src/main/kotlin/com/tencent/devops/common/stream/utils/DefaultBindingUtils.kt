package com.tencent.devops.common.stream.utils

import com.tencent.devops.common.api.util.UUIDUtil
import java.lang.reflect.Method

object DefaultBindingUtils {

    fun getOutBindingName(clazz: Class<*>) = clazz.simpleName.decapitalize()

    fun getInBindingName(method: Method, suffix: String?): String {
        return if (suffix.isNullOrBlank()) {
            method.name
        } else {
            "${method.name}-$suffix"
        }
    }

    fun getInBindingName(clazz: Class<*>, suffix: String?): String {
        return if (suffix.isNullOrBlank()) {
            clazz.simpleName.decapitalize()
        } else {
            "${clazz.simpleName.decapitalize()}-$suffix"
        }
    }
}
