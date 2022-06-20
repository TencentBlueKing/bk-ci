package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.expression.ExpressionConstants

class Wildcard : ExpressionNode() {
    override val traceFullyRealized = false

    override fun convertToExpression(): String = ExpressionConstants.WILDCARD.toString()

    override fun convertToRealizedExpression(context: EvaluationContext): String =
        ExpressionConstants.WILDCARD.toString()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        return Pair(null, ExpressionConstants.WILDCARD)
    }
}
