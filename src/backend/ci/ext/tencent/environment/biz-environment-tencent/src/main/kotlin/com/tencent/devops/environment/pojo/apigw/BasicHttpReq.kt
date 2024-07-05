package com.tencent.devops.environment.pojo.apigw

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

open class BasicHttpReq {
    fun toUrlParams(): String? {
        val urlString = StringBuilder(512)
        val c = '&'
        var aClass: Class<*>? = javaClass
        while (aClass != null) {
            val declaredFields = aClass.declaredFields
            for (field in declaredFields) {
                var key: String?
                val annotation = field.getAnnotation(JsonProperty::class.java)
                if (annotation != null) {
                    key = annotation.value
                } else {
                    key = field.name
                }
                if (!field.isAccessible) {
                    field.isAccessible = true
                    try {
                        val b = field[this]
                        if (b != null) {
                            urlString.append(c).append(key).append('=').append(urlEncode(b.toString()))
                        }
                    } catch (ignored: IllegalAccessException) {
                    } finally {
                        field.isAccessible = false
                    }
                }
            }
            aClass = aClass.superclass
        }
        return if (urlString.toString().length > 0) {
            "?" + urlString.toString().substring(1)
        } else {
            "?"
        }
    }

    private fun urlEncode(str: String): String? {
        return try {
            URLEncoder.encode(str, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("encode failed")
        }
    }
}
