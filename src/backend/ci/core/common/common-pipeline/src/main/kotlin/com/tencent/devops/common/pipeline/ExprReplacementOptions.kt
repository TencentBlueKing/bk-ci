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

package com.tencent.devops.common.pipeline

import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.expression.ExpressionOutput
import com.tencent.devops.common.expression.expression.IFunctionInfo
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 表达式替换上下文
 */
@Schema(title = "表达式替换参数")
data class ExprReplacementOptions(
    @get:Schema(title = "环境变量", required = true)
    val contextMap: Map<String, String>,
    @get:Schema(title = "值是否能不存在", required = true)
    val contextNotNull: Boolean = false,
    @get:Schema(title = "表达式上下文", required = true)
    val contextPair: Pair<ExecutionContext, List<NamedValueInfo>>? = null,
    val functions: Iterable<IFunctionInfo>? = null,
    val output: ExpressionOutput? = null
)
