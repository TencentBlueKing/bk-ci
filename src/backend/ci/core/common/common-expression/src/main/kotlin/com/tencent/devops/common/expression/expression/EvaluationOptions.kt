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

package com.tencent.devops.common.expression.expression

/**
 * @param contextNotNull 上下文计算时需要不存在的变量抛出异常而不是返回空
 * @param maxMemory 暂未使用
 */
data class EvaluationOptions(
    val contextNotNull: ExceptionInsteadOfNullOption,
    var maxMemory: Int = 0
) {
    fun contextNotNull(): Boolean {
        return contextNotNull.enable
    }

    constructor(contextNotNull: Boolean) : this(
        if (contextNotNull) {
            ExceptionInsteadOfNullOption.enable()
        } else {
            ExceptionInsteadOfNullOption.disabled()
        }
    )
}

/**
 * @param enable 是否开启
 * @param exceptionTraceMsg 存放为空的变量的索引链路，方便排查
 */
data class ExceptionInsteadOfNullOption(
    val enable: Boolean,
    val exceptionTraceMsg: MutableList<String>?
) {
    fun trace(name: String) {
        if (!enable) {
            return
        }
        // 字符串的格式化会带 ''
        exceptionTraceMsg?.add(name.removeSurrounding("'"))
    }

    fun errKey() = exceptionTraceMsg?.joinToString(".")

    companion object {
        fun disabled(): ExceptionInsteadOfNullOption = ExceptionInsteadOfNullOption(false, null)
        fun enable(): ExceptionInsteadOfNullOption = ExceptionInsteadOfNullOption(true, mutableListOf())
    }
}