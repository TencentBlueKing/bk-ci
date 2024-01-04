package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.transfer.IfType

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

    fun simplifyParams(defaultValue: Map<String, String>?, input: Map<String, Any>): MutableMap<String, Any> {
        val out = input.toMutableMap()
        defaultValue?.forEach {
            val value = out[it.key]
            if (value is String && it.value == value) {
                out.remove(it.key)
            }
            if (value is Boolean && it.value == value.toString()) {
                out.remove(it.key)
            }
            // 单独针对list的情况
            if (value is List<*> && it.value == value.joinToString(separator = ",")) {
                out.remove(it.key)
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
