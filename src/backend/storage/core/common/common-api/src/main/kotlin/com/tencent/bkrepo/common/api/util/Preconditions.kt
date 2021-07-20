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

package com.tencent.bkrepo.common.api.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import java.util.regex.Pattern

/**
 * 前置条件校验工具类
 */
object Preconditions {

    /**
     * 校验[expression]一定为true，[name]为提示字段名称
     */
    fun checkArgument(expression: Boolean?, name: String) {
        if (expression != true) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, name)
        }
    }

    /**
     * 校验[value]不能为空，[name]为提示字段名称
     */
    fun checkNotNull(value: Any?, name: String) {
        if (value == null) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, name)
        }
    }

    /**
     * 校验对象[value]不能为空，[name]为提示字段名称
     * 1. 如果value 为 list、map等集合对象，则长度必须大于1
     * 2. 如何value 为 string, 则长度必须大于1且不能为全空
     */
    fun checkNotBlank(value: Any?, name: String) {
        when (value) {
            null -> throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, name)
            is String -> {
                if (value.isBlank()) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_EMPTY, name)
                }
            }
            is Collection<*> -> {
                if (value.isEmpty()) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_EMPTY, name)
                }
            }
            is Map<*, *> -> {
                if (value.isEmpty()) {
                    throw ErrorCodeException(CommonMessageCode.PARAMETER_EMPTY, name)
                }
            }
        }
    }

    /**
     * 校验[value]符合正则表达式[pattern]，[name]为提示字段名称
     */
    fun matchPattern(value: String?, pattern: String, name: String) {
        if (!Pattern.matches(pattern, value)) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, name)
        }
    }
}
