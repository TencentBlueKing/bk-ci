package com.tencent.devops.common.expression.expression.sdk.operators

import com.tencent.devops.common.expression.expression.sdk.Container
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class Not(override val traceFullyRealized: Boolean = false) : Container() {
    override fun convertToExpression(): String = "!${parameters[0].convertToExpression()}"

    override fun convertToRealizedExpression(context: EvaluationContext): String {
        // Check if the result was stored
        val (re, result) = context.tryGetTraceResult(this)
        if (result) {
            return re!!
        }

        return "!${parameters[0].convertToRealizedExpression(context)}"
    }

    override fun evaluateCore(
        context: EvaluationContext
    ): Pair<ResultMemory?, Any?> = Pair(null, parameters[0].evaluate(context).isFalsy)
}
