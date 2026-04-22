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

package com.tencent.devops.ai.context

/**
 * Supervisor 系统提示词中**动态上下文区间**的标记常量。
 *
 * Supervisor 智能体实例会被缓存复用，sysPrompt 中的 `{{context_block}}`
 * 占位符在首次创建时即被替换为真实值，后续对话找不到占位符。
 *
 * 通过在替换结果中包裹 [START] / [END] 标记，
 * [com.tencent.devops.ai.hook.ContextRefreshHook] 可在每次推理前
 * 用正则匹配标记区间并整体替换为最新上下文。
 */
object ContextMarker {

    /** 上下文区间起始标记 */
    const val START = "<!-- CONTEXT_START -->"

    /** 上下文区间结束标记 */
    const val END = "<!-- CONTEXT_END -->"

    /** 匹配从 [START] 到 [END]（含标记本身）的所有内容 */
    val PATTERN: Regex = Regex(
        """<!-- CONTEXT_START -->.*?<!-- CONTEXT_END -->""",
        RegexOption.DOT_MATCHES_ALL
    )
}
