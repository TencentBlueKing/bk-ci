package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.expression.EvaluationResult
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyArray
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class Contains : Function() {
    override val traceFullyRealized: Boolean
        get() = false

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val left = parameters[0].evaluate(context)
        if (left.isPrimitive) {
            val leftString = left.convertToString()

            val right = parameters[1].evaluate(context)
            if (right.isPrimitive) {
                val rightString = right.convertToString()
                return Pair(null, leftString.indexOf(rightString, ignoreCase = true) >= 0)
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
}
