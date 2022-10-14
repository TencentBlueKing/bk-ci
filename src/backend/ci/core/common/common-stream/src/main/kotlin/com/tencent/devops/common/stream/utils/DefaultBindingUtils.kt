package com.tencent.devops.common.stream.utils

import java.lang.reflect.Method

object DefaultBindingUtils {

    fun getOutBindingName(clazz: Class<*>) = clazz.simpleName.decapitalize()

    fun getInBindingName(method: Method): String = method.name

    fun getInBindingName(clazz: Class<*>) = clazz.simpleName.decapitalize()
}
