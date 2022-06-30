package com.tencent.devops.common.expression.expression

interface IExpressionNode {
    fun evaluate(
        trace: ITraceWriter?,
        state: Any?,
        options: EvaluationOptions?
    ): EvaluationResult
}
