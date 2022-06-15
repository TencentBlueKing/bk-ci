package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.sdk.Function

class FunctionInfo(
    override val name: String,
    override val minParameters: Int,
    override val maxParameters: Int,
    private val f: Function
) : IFunctionInfo {
    override fun createNode(): Function {
        return f
    }
}
