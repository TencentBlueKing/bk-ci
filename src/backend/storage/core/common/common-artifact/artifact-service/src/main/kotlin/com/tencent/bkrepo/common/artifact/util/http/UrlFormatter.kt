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

package com.tencent.bkrepo.common.artifact.util.http

import com.tencent.bkrepo.common.api.constant.CharPool.QUESTION
import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.api.constant.StringPool.HTTP
import com.tencent.bkrepo.common.api.constant.StringPool.HTTPS

/**
 * Http URL 格式化工具类
 */
object UrlFormatter {

    /**
     * 格式化url
     */
    fun format(host: String, uri: String? = null, query: String? = null): String {
        val builder = StringBuilder()
        builder.append(formatHost(host))
        if (!uri.isNullOrBlank()) {
            builder.append(uri.trim(SLASH))
        }
        if (!query.isNullOrBlank()) {
            builder.append(QUESTION).append(query.trim(SLASH))
        }
        return builder.toString()
    }

    /**
     * 格式化[host]
     * http://xxx.com/// -> http://xxx.com/
     */
    fun formatHost(host: String): String {
        return host.trim().trimEnd(SLASH).plus(SLASH)
    }

    /**
     * 格式化url
     */
    @Throws(IllegalArgumentException::class)
    fun formatUrl(value: String): String {
        var url = value.trim()
        if (url.isBlank()) {
            throw IllegalArgumentException("Url should not be blank")
        }
        if (!url.startsWith(HTTP) && !url.startsWith(HTTPS)) {
            url = HTTP + url
        }
        return url
    }
}
