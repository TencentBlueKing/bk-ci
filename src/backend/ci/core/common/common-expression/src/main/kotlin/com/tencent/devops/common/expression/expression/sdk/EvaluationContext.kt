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

import com.tencent.devops.common.expression.expression.EvaluationOptions
import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.ExpressionOutput
import com.tencent.devops.common.expression.expression.ITraceWriter

class EvaluationContext(
    val trace: ITraceWriter?,
    val state: Any?,
    ops: EvaluationOptions?,
    val node: ExpressionNode?,
    val expressionOutput: ExpressionOutput?
) {
    val options: EvaluationOptions = EvaluationOptions(ops)
    val memory: EvaluationMemory
    private val mTraceResults = mutableMapOf<ExpressionNode, String>()
    private val mTraceMemory: MemoryCounter

    init {
        if (options.maxMemory == 0) {
            // Set a reasonable default max memory
            options.maxMemory = 1048576 // 1 mb
        }

        memory = EvaluationMemory(options.maxMemory, node)

        mTraceMemory = MemoryCounter(null, options.maxMemory)
    }

    fun setTraceResult(
        node: ExpressionNode,
        result: EvaluationResult
    ) {
        // Remove if previously added. This typically should not happen. This could happen
        // due to a badly authored function. So we'll handle it and track memory correctly.
        val oldValue = mTraceResults[node]
        if (oldValue != null) {
            mTraceMemory.remove(oldValue)
            mTraceResults.remove(node)
        }

        // Check max memory
        val value = ExpressionUtility.formatValue(result)
        if (mTraceMemory.tryAdd(value)) {
            // Store the result
            mTraceResults[node] = value
        }
    }

    fun tryGetTraceResult(node: ExpressionNode): Pair<String?, Boolean> {
        val value = mTraceResults[node] ?: return Pair(null, false)
        return Pair(value, true)
    }
}
