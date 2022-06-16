package com.tencent.devops.common.expression.expression.sdk

abstract class Function : Container() {

    override val traceFullyRealized = true

    override fun convertToExpression(): String {
        return "$name(${parameters.joinToString(", ") { it.convertToExpression() }})"
    }

    override fun convertToRealizedExpression(context: EvaluationContext): String {
        val (value, result) = context.tryGetTraceResult(this)
        if (result) {
            return value!!
        }

        return "$name(${parameters.joinToString(", ") { it.convertToExpression() }})"
    }

    abstract fun createNode(): Function
}
