package com.tencent.devops.common.expression.expression.functions

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.EvaluationContext
import com.tencent.devops.common.expression.expression.sdk.Function
import com.tencent.devops.common.expression.expression.sdk.ResultMemory
import com.tencent.devops.common.expression.utils.ExpressionJsonUtil

class ToJson : Function() {
    companion object {
        const val name = "toJSON"
    }

    override fun createNode(): Function = ToJson()

    override fun evaluateCore(context: EvaluationContext): Pair<ResultMemory?, Any?> {
        val res = parameters[0].evaluate(context)
        if (res.value == null) {
            return null to null
        }
        val json = if (res.value is PipelineContextData) {
            ExpressionJsonUtil.write(res.value.toJson())
        } else {
            JsonUtil.toJson(res.value)
        }
        return Pair(null, StringContextData(json))
    }

    override fun subNameValueEvaluateCore(context: EvaluationContext): Pair<Any?, Boolean> {
        val left = parameters[0].subNameValueEvaluate(context).parseSubNameValueEvaluateResult()
        return Pair("$name($left)", false)
    }
}