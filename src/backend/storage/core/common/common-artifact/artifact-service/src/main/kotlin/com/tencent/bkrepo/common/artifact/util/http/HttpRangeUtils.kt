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

import com.tencent.bkrepo.common.artifact.stream.Range
import org.springframework.http.HttpHeaders
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest

/**
 * Http Range请求工具类
 */
object HttpRangeUtils {

    private val RANGE_HEADER_PATTERN = Pattern.compile("bytes=(\\d+)?-(\\d+)?")

    /**
     * 从[request]中解析Range，[total]代表总长度
     */
    @Throws(IllegalArgumentException::class)
    fun resolveRange(request: HttpServletRequest, total: Long): Range {
        val rangeHeader = request.getHeader(HttpHeaders.RANGE)?.trim()
        if (rangeHeader.isNullOrEmpty()) return Range.full(total)
        val matcher = RANGE_HEADER_PATTERN.matcher(rangeHeader)
        require(matcher.matches()) { "Invalid range header: $rangeHeader" }
        require(matcher.groupCount() >= 1) { "Invalid range header: $rangeHeader" }
        return if (matcher.group(1).isNullOrEmpty()) {
            val start = total - matcher.group(2).toLong()
            val end = total - 1
            Range(start, end, total)
        } else {
            val start = matcher.group(1).toLong()
            val end = if (matcher.group(2).isNullOrEmpty()) total - 1 else matcher.group(2).toLong()
            Range(start, end, total)
        }
    }
}
