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

package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyArray
import com.tencent.devops.common.expression.expression.sdk.MemoryCounter
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

@Suppress("NestedBlockDepth", "ReturnCount")
class Join : Function() {
    companion object {
        const val name = "join"
    }

    override fun createNode(): Function = Join()

    override val traceFullyRealized: Boolean
        get() = true

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val items = parameters[0].evaluate(context)

        // Array
        val (collection, ok) = items.tryGetCollectionInterface()
        if (ok && collection is IReadOnlyArray<*> && collection.count > 0) {
            val result = StringBuilder()
            val memory = MemoryCounter(this, context.options.maxMemory)

            // Append the first item
            val item = collection[0]
            val itemResult = EvaluationResult.createIntermediateResult(context, item)
            val itemString = itemResult.convertToString()
            memory.add(itemString)
            result.append(itemString)

            // More items?
            if (collection.count > 1) {
                var separator = ","
                if (parameters.count() > 1) {
                    val separatorResult = parameters[1].evaluate(context)
                    if (separatorResult.isPrimitive) {
                        separator = separatorResult.convertToString()
                    }
                }

                (1 until collection.count).forEach { i ->
                    // Append the separator
                    memory.add(separator)
                    result.append(separator)

                    // Append the next item
                    val nextItem = collection[i]
                    val nextItemResult = EvaluationResult.createIntermediateResult(context, nextItem)
                    val nextItemString = nextItemResult.convertToString()
                    memory.add(nextItemString)
                    result.append(nextItemString)
                }
            }

            return Pair(ResultMemory().also { it.bytes = memory.currentBytes }, result.toString())
        }
        // Primitive
        else if (items.isPrimitive) {
            return Pair(null, items.convertToString())
        }
        // Otherwise return empty string
        else {
            return Pair(null, "")
        }
    }

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val left = parameters[0].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
        if (parameters.size == 1) {
            return Pair("$name($left)", false)
        }
        val right = parameters[1].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
        return Pair("$name($left, $right)", false)
    }
}
