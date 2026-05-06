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

package com.tencent.devops.process.pojo

/**
 * 构建变量快照。
 *
 * 包含两类信息：
 * 1. [smallVars]：所有不超过 4K 的变量（含被截断的"摘要值"）。
 *    保持兼容老的 `$xxx`、`${xxx}` 替换逻辑：旧脚本最多看到摘要值，
 *    需要完整值时必须使用 `${{ xxx }}` 表达式语法。
 * 2. [largeKeys]：本次构建中存在溢出的变量名集合。
 * 3. [largeValueLoader]：当 ${{ xxx }} 表达式访问溢出变量时按需调用，
 *    避免一次性把所有大值加载进内存。
 */
data class BuildVariableSnapshot(
    val smallVars: Map<String, String>,
    val largeKeys: Set<String>,
    val largeValueLoader: (String) -> String?
) {
    /** 同 [smallVars]，便于向后兼容旧 API 直接以 [Map] 返回。 */
    val asMap: Map<String, String> get() = smallVars

    /** 是否有溢出的大变量。 */
    val hasOverflow: Boolean get() = largeKeys.isNotEmpty()

    /**
     * 获取变量值。如果 [key] 是溢出键，则按需加载完整值；否则直接读 [smallVars]。
     * 注意：该方法只供"少量、单点"调用使用，不要在循环中遍历全部 [largeKeys] —
     * 这会等价于一次性把所有大值加载到内存，违背"按需加载"的初衷。
     */
    fun resolve(key: String): String? {
        if (key in largeKeys) {
            return largeValueLoader.invoke(key)
        }
        return smallVars[key]
    }
}
