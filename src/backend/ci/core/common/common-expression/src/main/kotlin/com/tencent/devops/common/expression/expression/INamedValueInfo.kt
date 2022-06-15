package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.expression.sdk.NamedValue

interface INamedValueInfo {
    val name: String
    fun createNode(): NamedValue
}
