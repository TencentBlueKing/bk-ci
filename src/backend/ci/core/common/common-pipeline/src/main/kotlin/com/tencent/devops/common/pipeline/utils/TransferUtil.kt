package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.transfer.IfType
import org.json.JSONObject

object TransferUtil {
    fun parseStepIfFiled(
        step: Element
    ): String? {
        return when (step.additionalOptions?.runCondition) {
            RunCondition.CUSTOM_CONDITION_MATCH -> step.additionalOptions?.customCondition
            RunCondition.CUSTOM_VARIABLE_MATCH -> customVariableMatch(
                step.additionalOptions?.customVariables
            )

            RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> customVariableMatchNotRun(
                step.additionalOptions?.customVariables
            )

            RunCondition.PRE_TASK_FAILED_BUT_CANCEL ->
                IfType.ALWAYS_UNLESS_CANCELLED.name

            RunCondition.PRE_TASK_FAILED_EVEN_CANCEL ->
                IfType.ALWAYS.name

            RunCondition.PRE_TASK_FAILED_ONLY ->
                IfType.FAILURE.name

            else -> null
        }
    }

    /*
    * 简化input, 如果是默认值则去掉
    * */
    fun simplifyParams(defaultValue: JSONObject?, input: Map<String, Any>): MutableMap<String, Any> {
        val out = input.toMutableMap()
        defaultValue?.keys()?.forEach { key ->
            val inputValue = out[key] ?: return@forEach
            if (JSONObject(key to defaultValue[key]).similar(JSONObject(key to inputValue))) {
                out.remove(key)
            }
        }
        return out
    }

    /*
    * 填充input，如果input没有，defaultValueMap有，则填充进去。
    * */
    fun mixParams(defaultValue: JSONObject?, input: Map<String, Any?>?): MutableMap<String, Any?> {
        val out = input?.toMutableMap() ?: mutableMapOf()
        defaultValue?.toMap()?.forEach { (k, v) ->
            val value = out[k]
            if (value == null) {
                out[k] = v
            }
        }
        return out
    }

    fun customVariableMatchNotRun(input: List<NameAndValue>?): String? {
        val ifString = input?.joinToString(separator = " || ") {
            "${it.key} != '${it.value}' "
        }
        return if (input?.isEmpty() == true) null
        else ifString
    }

    fun customVariableMatch(input: List<NameAndValue>?): String? {
        val ifString = input?.joinToString(separator = " && ") {
            "${it.key} == '${it.value}' "
        }
        return if (input?.isEmpty() == true) null
        else ifString
    }
}
