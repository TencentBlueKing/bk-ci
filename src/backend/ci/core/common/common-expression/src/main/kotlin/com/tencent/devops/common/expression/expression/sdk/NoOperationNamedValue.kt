package com.tencent.devops.common.expression.expression.sdk

class NoOperationNamedValue : NamedValue() {
    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        return Pair(null, null)
    }
}
