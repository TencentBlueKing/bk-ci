/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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

package com.tencent.devops.log.util

/**
 * 日志下载 Content-Disposition，避免 query fileName 注入响应头。
 */
object LogDownloadHeader {

    private val UNSAFE_CHARS = Regex("[^A-Za-z0-9._-]")
    private const val FALLBACK_NAME = "build-log"

    fun contentDisposition(fileName: String?, pipelineId: String, buildId: String): String {
        val stem = sanitize(fileName?.takeIf { it.isNotBlank() } ?: "$pipelineId-$buildId-log")
            .ifBlank { sanitize("$pipelineId-$buildId-log") }
            .ifBlank { FALLBACK_NAME }
        val asciiName = "$stem.log"
        return "attachment; filename=\"$asciiName\"; filename*=UTF-8''${encodeRfc5987(asciiName)}"
    }

    internal fun sanitize(value: String): String {
        return value.replace("\r", "")
            .replace("\n", "")
            .replace("\"", "")
            .replace(UNSAFE_CHARS, "")
    }

    internal fun encodeRfc5987(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size)
        for (b in bytes) {
            val unsigned = b.toInt() and 0xFF
            val c = unsigned.toChar()
            if (c.isLetterOrDigit() || c == '.' || c == '_' || c == '-') {
                sb.append(c)
            } else {
                sb.append('%').append("%02X".format(unsigned))
            }
        }
        return sb.toString()
    }
}
