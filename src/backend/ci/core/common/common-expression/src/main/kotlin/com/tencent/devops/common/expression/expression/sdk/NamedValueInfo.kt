package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.expression.INamedValueInfo

class NamedValueInfo(
    override val name: String,
    private val ob: NamedValue
) : INamedValueInfo {

    override fun createNode(): NamedValue {
        return ob
    }
}
