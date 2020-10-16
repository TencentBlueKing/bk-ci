/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.api.constant

import java.util.UUID

object StringPool {
    const val EMPTY = ""
    const val DOT = "."
    const val SLASH = "/"
    const val ROOT = SLASH
    const val COLON = ":"
    const val DASH = "-"
    const val HTTP = "http://"
    const val HTTPS = "https://"
    const val UNKNOWN = "Unknown"
    const val TEMP = "temp"
    const val UTF_8 = "UTF-8"
    const val BYTES = "bytes"
    const val NO_CACHE = "no-cache"

    private val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun randomString(size: Int) = List(size) { alphabet.random() }.joinToString("")

    fun uniqueId() = UUID.randomUUID().toString().replace(DASH, EMPTY).toLowerCase()
}
