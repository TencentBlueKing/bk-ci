package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class NoOperation : Function() {
    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        return Pair(null, null)
    }

    override fun createNode(): Function {
        return NoOperation()
    }
}
