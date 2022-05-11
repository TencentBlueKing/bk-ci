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

package com.tencent.bkrepo.common.storage.innercos

import java.net.URLEncoder
import java.util.concurrent.TimeUnit

const val DEFAULT_ENCODING = "UTF-8"
const val PATH_DELIMITER = '/'
const val PARAMETER_UPLOAD_ID = "uploadid"
const val PARAMETER_PART_NUMBER = "partnumber"
const val PARAMETER_UPLOADS = "uploads"
const val COS_COPY_SOURCE = "x-cos-copy-source"

const val RESPONSE_UPLOAD_ID = "UploadId"
const val RESPONSE_LAST_MODIFIED = "LastModified"

private const val ENCODED_STR_SIZE = 3

fun String.urlEncode(toLower: Boolean): String {
    val encodedString = URLEncoder.encode(this, DEFAULT_ENCODING)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")
    if (!toLower) {
        return encodedString
    }
    val builder = StringBuilder()
    val length = encodedString.length
    var index = 0
    while (index < length) {
        index += if (encodedString[index] == '%' && index + 2 < length) {
            builder.append(encodedString[index])
            builder.append(Character.toLowerCase(encodedString[index + 1]))
            builder.append(Character.toLowerCase(encodedString[index + 2]))
            ENCODED_STR_SIZE
        } else {
            builder.append(encodedString[index])
            1
        }
    }
    return builder.toString()
}

/**
 * 重试函数，times表示重试次数，加上第一次执行，总共会执行times+1次，
 */
@Suppress("TooGenericExceptionCaught") // 无法预知block具体异常类型
inline fun <R> retry(times: Int, delayInSeconds: Long = 5, block: (Int) -> R): R {
    var retries = 0
    while (true) {
        try {
            return block(retries)
        } catch (e: Exception) {
            if (retries < times) {
                TimeUnit.SECONDS.sleep(delayInSeconds)
                retries += 1
            } else {
                throw e
            }
        }
    }
}
