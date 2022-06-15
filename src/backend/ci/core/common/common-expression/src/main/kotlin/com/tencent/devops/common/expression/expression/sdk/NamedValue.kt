package com.tencent.devops.common.expression.expression.sdk

abstract class NamedValue : ExpressionNode() {
    override val traceFullyRealized = true

    override fun convertToExpression() = name

    override fun convertToRealizedExpression(context: EvaluationContext): String {
        val (value, result) = context.tryGetTraceResult(this)
        if (result) {
            return value!!
        }

        return name
    }
}
