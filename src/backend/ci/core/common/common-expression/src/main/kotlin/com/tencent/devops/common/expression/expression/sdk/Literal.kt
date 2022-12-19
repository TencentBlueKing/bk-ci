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

import com.tencent.devops.common.expression.expression.ValueKind

class Literal(v: Any?) : ExpressionNode() {
    val value: Any?
    val kind: ValueKind

    init {
        val (k, _, va) = ExpressionUtility.convertToCanonicalValue(v)
        value = va
        kind = k
        name = name
    }

    // 防止将值存储在评估上下文中。
    // 这样可以避免不必要地复制内存中的值。
    override val traceFullyRealized = false

    override fun convertToExpression() = ExpressionUtility.formatValue(value, kind)

    override fun convertToRealizedExpression(context: EvaluationContext) = ExpressionUtility.formatValue(value, kind)

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> = Pair(null, value)

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val v = when (kind) {
            ValueKind.Null -> null

            ValueKind.Boolean -> value as Boolean

            ValueKind.Number -> value as Double

            ValueKind.String -> ExpressionUtility.stringEscape(value as String)

            ValueKind.Array, ValueKind.Object -> kind.toString()
        }

        return Pair(v, true)
    }
}
