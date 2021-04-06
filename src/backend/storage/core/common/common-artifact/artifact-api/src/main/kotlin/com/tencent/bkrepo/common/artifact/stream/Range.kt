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

package com.tencent.bkrepo.common.artifact.stream

import kotlin.math.min

/**
 * 文件范围
 * @param startPosition 起始位置，从0开始
 * @param endPosition 结束位置，最大值为[total]-1
 * @param total 文件总长度
 */
class Range(startPosition: Long, endPosition: Long, val total: Long) {

    /**
     * 起始位置
     */
    val start: Long = if (startPosition < 0) 0 else startPosition

    /**
     * 结束位置，范围为[start, total-1]，如果超出返回则设置为[total] - 1
     */
    val end: Long = if (endPosition < 0) total - 1 else min(endPosition, total - 1)

    /**
     * 范围长度
     */
    val length: Long = end - start + 1

    init {
        require(total >= 0) { "Invalid total size: $total" }
        require(length >= 0) { "Invalid range length $length" }
    }

    /**
     * 是否为部分内容
     */
    fun isPartialContent(): Boolean {
        return length != total
    }

    /**
     * 是否为完整内容
     */
    fun isFullContent(): Boolean {
        return length == total
    }

    /**
     * 是否为空内容
     */
    fun isEmpty(): Boolean {
        return length == 0L
    }

    override fun toString(): String {
        return "$start-$end/$total"
    }

    companion object {
        /**
         * 创建长度为[total]的完整范围
         */
        fun full(total: Long) = Range(0, total - 1, total)
    }
}
