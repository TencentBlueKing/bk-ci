package com.tencent.devops.common.expression.expression.sdk.operators

import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.sdk.Container
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class And(override val traceFullyRealized: Boolean = false) : Container() {
    override fun convertToExpression(): String {
        return "(${parameters.joinToString(" && ") { it.convertToExpression() }})"
    }

    override fun convertToRealizedExpression(context: EvaluationContext): String {
        // Check if the result was stored
        val (re, result) = context.tryGetTraceResult(this)
        if (result) {
            return re!!
        }

        return "(${parameters.joinToString(" && ") { it.convertToExpression() }})"
    }

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        var result: EvaluationResult? = null
        parameters.forEach { parameter ->
            result = parameter.evaluate(context)
            if (result?.isFalsy == true) {
                return Pair(null, result!!.value)
            }
        }

        return Pair(null, result?.value)
    }
}
