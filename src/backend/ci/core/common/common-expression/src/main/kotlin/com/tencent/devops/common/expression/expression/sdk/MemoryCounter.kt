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

package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.InvalidOperationException
import com.tencent.devops.common.expression.resources.ExpressionResources

/**
 * ExpressionNode 的帮助类。 此类有助于计算结果对象的内存开销。
 */
@Suppress("ReturnCount")
class MemoryCounter(val node: ExpressionNode?, maxBytes: Int?) {
    private val mMaxBytes: Int
    private var mCurrentBytes: Int = 0
    val currentBytes: Int
        get() = mCurrentBytes

    init {
        mMaxBytes = if ((maxBytes ?: 0) > 0) {
            maxBytes!!
        } else {
            Int.MAX_VALUE
        }
    }

    fun add(amount: Int) {
        if (!tryAdd(amount)) {
            throw InvalidOperationException(ExpressionResources.exceededAllowedMemory(node?.convertToExpression()))
        }
    }

    fun add(value: String) {
        add(calculateSize(value))
    }

    fun addMinObjectSize() {
        add(minObjectSize)
    }

    fun remove(value: String?) {
        mCurrentBytes -= calculateSize(value)
    }

    private fun tryAdd(amount: Int): Boolean {
        try {
            val a = EvaluationMemory.checked {
                amount + mCurrentBytes
            }

            if (a > mMaxBytes) {
                return false
            }

            mCurrentBytes = amount
            return true
        }
        // c# OverflowException
        catch (ignore: Exception) {
            return false
        }
    }

    fun tryAdd(value: String?): Boolean {
        return tryAdd(calculateSize(value))
    }

    companion object {
        private const val minObjectSize = 24
        private const val stringBaseOverhead = 26

        fun calculateSize(value: String?): Int {
            // This measurement doesn't have to be perfect.
            // https://codeblog.jonskeet.uk/2011/04/05/of-memory-and-strings/

            val bytes = EvaluationMemory.checked {
                stringBaseOverhead + ((value?.length ?: 0) * 2)
            }
            return bytes
        }
    }
}
