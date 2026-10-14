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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.util.EnvUtils

/**
 * 并发组同一批标识。
 *
 * 没有该字段的存量配置与旧客户端请求保持原两档行为。
 * [normalize] 里 null 表示请求未携带，沿用已保存的值；空白表示显式清空。
 */
object ConcurrencySubGroupPolicy {

    fun normalize(incoming: String?, existing: String?): String? {
        return when {
            incoming == null -> existing
            incoming.isBlank() -> null
            else -> incoming
        }
    }

    /**
     * 能用来区分同一批的标识。空白和字面量 null 都视为没有标识。
     */
    fun effectiveBatchId(value: String?): String? {
        val text = value?.trim() ?: return null
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) {
            return null
        }
        return text
    }

    /**
     * 启动时解析表达式。
     * 未配置返回 null，由取消开关决定整组取消或排队。
     * 配置了但解析为 null、空白时返回空串：这次不按同一批取消，进入队列按顺序运行。
     */
    fun resolve(expression: String?, variables: Map<String, String>): String? {
        if (expression.isNullOrBlank()) {
            return null
        }
        val resolved = EnvUtils.parseEnv(
            command = expression,
            data = variables,
            replaceWithEmpty = true
        ).trim()
        return effectiveBatchId(resolved) ?: ""
    }
}
