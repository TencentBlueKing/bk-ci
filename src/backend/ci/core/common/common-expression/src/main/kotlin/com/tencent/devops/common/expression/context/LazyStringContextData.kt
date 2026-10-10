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

package com.tencent.devops.common.expression.context

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.tencent.devops.common.expression.expression.sdk.IString

/**
 * 懒加载字符串上下文。
 *
 * 用于流水线大变量场景：当变量值超过主表 4K 限制时，主表只存"摘要"，
 * 完整值存在溢出表中。表达式 ${{ xxx }} 真正访问该值时，才通过 [supplier]
 * 加载完整值，避免一次性把全部大变量加载进内存。
 *
 * - [supplier] 仅在第一次取值时被调用，取到的值会被缓存以便后续重复访问；
 * - 当 supplier 返回 null 时，回退到 [fallback]（默认空字符串），
 *   保持与 [StringContextData] 的语义一致。
 */
class LazyStringContextData(
    private val supplier: () -> String?,
    private val fallback: String = ""
) : PipelineContextData(PipelineContextDataType.STRING), IString {

    @Volatile
    private var resolved: String? = null

    private fun resolve(): String {
        val cached = resolved
        if (cached != null) return cached
        val v = (supplier.invoke() ?: fallback)
        resolved = v
        return v
    }

    override fun getString(): String = resolve()

    override fun clone(): PipelineContextData = LazyStringContextData(supplier, fallback).also {
        it.resolved = this.resolved
    }

    override fun toJson(): JsonNode = TextNode(resolve())

    override fun fetchValue(): Any = resolve()

    override fun toString(): String = resolve()
}
