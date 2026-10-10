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

package com.tencent.devops.process.service

import com.tencent.devops.process.pojo.BuildVariableSnapshot

/**
 * 一次表达式求值会话：携带变量 Map + 大变量 overflowKeys/loader。
 *
 * 生命周期与"一次任务参数解析 / 一次条件求值"对齐，**不要长期持有**。
 */
data class BuildVarExprSession(
    val variables: Map<String, String>,
    val overflowKeys: Set<String>,
    val overflowLoader: ((String) -> String?)?
) {
    companion object {
        fun fromSnapshot(snapshot: BuildVariableSnapshot): BuildVarExprSession {
            val loader = if (snapshot.largeKeys.isEmpty()) {
                null
            } else {
                snapshot.largeValueLoader
            }
            return BuildVarExprSession(
                variables = snapshot.smallVars,
                overflowKeys = snapshot.largeKeys,
                overflowLoader = loader
            )
        }

        fun empty(variables: Map<String, String> = emptyMap()): BuildVarExprSession {
            return BuildVarExprSession(
                variables = variables,
                overflowKeys = emptySet(),
                overflowLoader = null
            )
        }
    }
}
