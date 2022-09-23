package com.tencent.devops.common.stream.utils

import java.lang.reflect.Method

object DefaultBindingUtils {

    fun getOutBindingName(clazz: Class<*>) = "${clazz.simpleName.decapitalize()}Out"

    fun getInBindingName(method: Method) = "${method.name}In"

    fun getInBindingName(clazz: Class<*>) = "${clazz.simpleName.decapitalize()}In"
}
