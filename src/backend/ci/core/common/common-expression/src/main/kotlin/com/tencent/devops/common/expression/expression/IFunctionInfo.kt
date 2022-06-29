package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.sdk.Function

interface IFunctionInfo {
    val name: String
    val minParameters: Int
    val maxParameters: Int
    fun createNode(): Function
}
