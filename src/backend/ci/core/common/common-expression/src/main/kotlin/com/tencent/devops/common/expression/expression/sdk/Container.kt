package com.tencent.devops.common.expression.expression.sdk

abstract class Container : ExpressionNode() {
    private val mParameters = mutableListOf<ExpressionNode>()

    val parameters
        get() = mParameters.toList()

    fun addParameters(node: ExpressionNode) {
        mParameters.add(node)
        node.container = this
    }
}
