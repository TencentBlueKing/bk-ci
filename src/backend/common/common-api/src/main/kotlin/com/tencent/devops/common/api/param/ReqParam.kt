package com.tencent.devops.common.api.param

import com.fasterxml.jackson.annotation.JsonProperty

interface ReqParam {

    fun beanToMap(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        var aClass: Class<*>? = this.javaClass
        while (aClass != null) {
            val declaredFields = aClass.declaredFields
            for (field in declaredFields) {
                val key: String
                val annotation = field.getDeclaredAnnotation(JsonProperty::class.java)
                key = annotation?.value ?: field.name
                if (!field.isAccessible) {
                    field.isAccessible = true
                    try {
                        val b = field.get(this)
                        if (b != null && b != "") {
                            result[key] = b.toString()
                        }
                    } catch (ignored: IllegalAccessException) {
                    } finally {
                        field.isAccessible = false
                    }
                }
            }
            aClass = aClass.superclass
        }
        return result
    }
}