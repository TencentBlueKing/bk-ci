package com.tencent.devops.common.expression.expression.sdk.operators

import com.tencent.devops.common.expression.expression.sdk.Container
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class Equal(override val traceFullyRealized: Boolean = false) : Container() {
    override fun convertToExpression(): String {
        return "(${parameters[0].convertToExpression()} == ${parameters[1].convertToExpression()})"
    }

    override fun convertToRealizedExpression(context: EvaluationContext): String {
        // Check if the result was stored
        val (re, result) = context.tryGetTraceResult(this)
        if (result) {
            return re!!
        }

        return "(${parameters[0].convertToExpression()} == ${parameters[1].convertToExpression()})"
    }

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val left = parameters[0].evaluate(context)
        val right = parameters[1].evaluate(context)
        return Pair(null, left.abstractEqual(right))
    }
}
