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

package com.tencent.devops.common.archive.util

import org.springframework.boot.web.server.MimeMappings

object MimeUtil {
    const val YAML_MIME_TYPE = "application/x-yaml"
    const val TGZ_MIME_TYPE = "application/x-tar"
    const val ICO_MIME_TYPE = "image/x-icon"
    const val STREAM_MIME_TYPE = "application/octet-stream"
    private const val HTML_MIME_TYPE = "text/html"

    private val mimeMappings = MimeMappings.lazyCopy(MimeMappings.DEFAULT).apply {
        add("yaml", YAML_MIME_TYPE)
        add("tgz", TGZ_MIME_TYPE)
        add("ico", ICO_MIME_TYPE)
        add("html", HTML_MIME_TYPE)
    }

    fun mediaType(fileName: String): String {
        val ext = fileName.trim().substring(fileName.lastIndexOf(".") + 1)
        return mimeMappings.get(ext) ?: STREAM_MIME_TYPE
    }
}
