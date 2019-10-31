package com.tencent.devops.notify.blueking.utils

import java.io.UnsupportedEncodingException
import java.util.ArrayList

object ChineseStringUtil {

    fun split(src: String?, bytes: Int): List<String>? {
        if (src.isNullOrEmpty()) {
            return null
        }
        val splitList = ArrayList<String>()
        var startIndex = 0 // 字符串截取起始位置
        var endIndex = if (bytes > src!!.length) src.length else bytes // 字符串截取结束位置
        while (startIndex < src.length) {
            var subString = src.substring(startIndex, endIndex)
            // 截取的字符串的字节长度大于需要截取的长度时，说明包含中文字符
            // 在GBK编码中，一个中文字符占2个字节，UTF-8编码格式，一个中文字符占3个字节。
            try {
                while (subString.toByteArray(charset("GBK")).size > bytes) {
                    --endIndex
                    subString = src.substring(startIndex, endIndex)
                }
            } catch (e: UnsupportedEncodingException) {
                return null
            }

            splitList.add(src.substring(startIndex, endIndex))
            startIndex = endIndex
            // 判断结束位置时要与字符串长度比较(src.length())，之前与字符串的bytes长度比较了，导致越界异常。
            endIndex = if (startIndex + bytes > src.length)
                src.length
            else
                startIndex + bytes
        }
        return splitList
    }
}