package com.tencent.devops.common.expression

import com.tencent.devops.common.expression.pipeline.contextData.DictionaryContextData

data class ExecutionContext(
    val expressionValues: DictionaryContextData
)
