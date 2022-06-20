package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.ResultMemory

class NoOperation : Function() {
    override fun createNode(): Function = NoOperation()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        return Pair(null, null)
    }
}
