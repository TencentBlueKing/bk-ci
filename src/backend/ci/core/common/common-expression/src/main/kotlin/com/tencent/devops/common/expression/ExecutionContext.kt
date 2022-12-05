package com.tencent.devops.common.expression

import com.tencent.devops.common.expression.context.DictionaryContextData

data class ExecutionContext(
    val expressionValues: DictionaryContextData
)
