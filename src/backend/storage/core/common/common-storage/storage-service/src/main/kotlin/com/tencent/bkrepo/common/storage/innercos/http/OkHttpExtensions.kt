/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.storage.innercos.http

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.Util.checkOffsetAndCount
import okio.BufferedSink
import java.io.Closeable
import java.nio.charset.Charset

fun String.toRequestBody(contentType: MediaType? = null): RequestBody {
    var charset: Charset = Charsets.UTF_8
    var finalContentType: MediaType? = contentType
    if (contentType != null) {
        val resolvedCharset = contentType.charset()
        if (resolvedCharset == null) {
            charset = Charsets.UTF_8
            finalContentType = "$contentType; charset=utf-8".toMediaTypeOrNull()
        } else {
            charset = resolvedCharset
        }
    }
    val bytes = toByteArray(charset)
    return bytes.toRequestBody(finalContentType, 0, bytes.size)
}

fun ByteArray.toRequestBody(
    contentType: MediaType? = null,
    offset: Int = 0,
    byteCount: Int = size
): RequestBody {
    checkOffsetAndCount(size.toLong(), offset.toLong(), byteCount.toLong())
    return object : RequestBody() {
        override fun contentType() = contentType

        override fun contentLength() = byteCount.toLong()

        override fun writeTo(sink: BufferedSink) {
            sink.write(this@toRequestBody, offset, byteCount)
        }
    }
}

fun String.toMediaTypeOrNull(): MediaType? {
    return try {
        MediaType.parse(this)
    } catch (_: IllegalArgumentException) {
        null
    }
}

inline fun <T : Closeable?, R> T.useOnCondition(condition: Boolean, block: (T) -> R): R {
    return if (condition) {
        use(block)
    } else {
        block(this)
    }
}
