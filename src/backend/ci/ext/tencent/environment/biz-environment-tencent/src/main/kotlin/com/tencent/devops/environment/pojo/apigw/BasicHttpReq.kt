package com.tencent.devops.environment.pojo.apigw

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.UnsupportedEncodingException
import java.lang.reflect.Field
import java.net.URLEncoder

open class BasicHttpReq {
    /**
     * 将请求体内容转换为URL参数
     */
    fun toUrlParams(): String {
        val urlString = StringBuilder(512)
        var aClass: Class<*>? = javaClass
        while (aClass != null) {
            val declaredFields = aClass.declaredFields
            for (field in declaredFields) {
                addFieldToUrlString(field, urlString)
            }
            aClass = aClass.superclass
        }
        return if (urlString.toString().isNotEmpty()) {
            "?" + urlString.toString().substring(1)
        } else {
            "?"
        }
    }

    private fun addFieldToUrlString(field: Field, urlString: StringBuilder) {
        val paramSeparateChar = '&'
        val key: String?
        val annotation = field.getAnnotation(JsonProperty::class.java)
        key = annotation?.value ?: field.name
        if (!field.isAccessible) {
            field.isAccessible = true
            try {
                val b = field[this]
                if (b != null) {
                    urlString.append(paramSeparateChar).append(key).append('=').append(urlEncode(b.toString()))
                }
            } catch (ignored: IllegalAccessException) {
            } finally {
                field.isAccessible = false
            }
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
