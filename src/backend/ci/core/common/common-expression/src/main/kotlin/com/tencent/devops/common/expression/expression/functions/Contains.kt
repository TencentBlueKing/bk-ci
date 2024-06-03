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
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

@Suppress("NestedBlockDepth", "ReturnCount")
class Contains : Function() {
    companion object {
        const val name = "contains"
    }

    override val traceFullyRealized: Boolean
        get() = false

    override fun createNode(): Function = Contains()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val left = parameters[0].evaluate(context)
        if (left.isPrimitive) {
            val leftString = left.convertToString()

            val right = parameters[1].evaluate(context)
            if (right.isPrimitive) {
                val rightString = right.convertToString()
                return Pair(null, leftString.indexOf(rightString, ignoreCase = false) >= 0)
            }
        } else {
            val (collection, ok) = left.tryGetCollectionInterface()
            if (ok && collection is IReadOnlyArray<*> && collection.count > 0) {
                val right = parameters[1].evaluate(context)
                collection.forEach { item ->
                    val itemResult = EvaluationResult.createIntermediateResult(context, item)
                    if (right.abstractEqual(itemResult)) {
                        return Pair(null, true)
                    }
                }
            }
        }

        return Pair(null, false)
    }

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val left = parameters[0].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
        val right = parameters[1].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
        return Pair("$name($left, $right)", false)
    }
}
