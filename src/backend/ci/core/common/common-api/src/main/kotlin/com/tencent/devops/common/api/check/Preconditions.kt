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

package com.tencent.devops.common.api.check

import com.tencent.devops.common.api.exception.ParamBlankException

/**
 * 前置条件校验工具类
 */
object Preconditions {

    /**
     * 检查对象[obj]不为空，否则抛出指定的异常[exception]
     */
    @Throws(Exception::class)
    fun <T : Any> checkNotNull(obj: T?, exception: () -> Exception): T {
        if (obj == null) {
            throw exception()
        }
        return obj
    }

    /**
     * 检查对象[obj]不为空，抛出默认错误内容
     */
    @Throws(Exception::class)
    fun <T : Any> checkNotNull(obj: T?): T {
        return checkNotNull(obj) { ParamBlankException("Required value was null.") }
    }

    /**
     * 检查对象[obj]不为空, 为空则抛出[message]内容提醒上游
     */
    @Throws(Exception::class)
    fun <T : Any> checkNotNull(obj: T?, message: String): T {
        return checkNotNull(obj) { ParamBlankException(message) }
    }

    /**
     * [condition]为true，否则抛出指定的异常[exception]
     */
    @Throws(Exception::class)
    fun checkTrue(condition: Boolean, exception: Exception) {
        if (!condition) {
            throw exception
        }
    }
}
