/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.notify.utils

import java.io.UnsupportedEncodingException
import java.util.ArrayList

@Suppress("ALL")
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
