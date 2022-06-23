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
}
