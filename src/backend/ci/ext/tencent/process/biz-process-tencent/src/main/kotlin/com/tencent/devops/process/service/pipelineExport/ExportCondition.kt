package com.tencent.devops.process.service.pipelineExport

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.pojo.MarketBuildAtomElementWithLocation
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.v2.models.IfType
import org.slf4j.LoggerFactory

object ExportCondition {

    private val logger = LoggerFactory.getLogger(ExportCondition::class.java)

    fun parseNameAndValueWithAnd(
        context: PipelineExportContext,
        nameAndValueList: List<NameAndValue>? = emptyList(),
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?
    ): String {

        var ifString = ""
        nameAndValueList?.forEachIndexed { index, nameAndValue ->
            val preStr = parseNameAndValueWithPreStr(
                context = context,
                nameAndValue = nameAndValue,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
            )
            ifString += if (index == nameAndValueList.size - 1) {
                "$preStr == '${nameAndValue.value}'"
            } else {
                "$preStr == '${nameAndValue.value}' && "
            }
        }
        return ifString
    }

    fun parseNameAndValueWithPreStr(
        context: PipelineExportContext,
        nameAndValue: NameAndValue,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?
    ): String {
        val stepElement = context.output2Elements[nameAndValue.key]
        val ciName = PipelineVarUtil.fetchReverseVarName("${nameAndValue.key}")
        return if (stepElement != null) {
            var lastExistingOutputElements = MarketBuildAtomElementWithLocation()
            val keyStr = nameAndValue.key ?: ""
            run outside@{
                stepElement.reversed().forEach lit@{
                    if (it.jobLocation?.id == pipelineExportV2YamlConflictMapItem?.job?.id ||
                        it.stageLocation?.id != pipelineExportV2YamlConflictMapItem?.stage?.id
                    ) {
                        if (it.stepAtom?.id == pipelineExportV2YamlConflictMapItem?.step?.id)
                            return@lit
                        lastExistingOutputElements = it
                        return@outside
                    }
                }
            }
            val namespace = lastExistingOutputElements.stepAtom?.data?.get("namespace") as String?
            val originKeyWithNamespace = if (!namespace.isNullOrBlank()) {
                keyStr.replace("${namespace}_", "")
            } else keyStr

            when {
                lastExistingOutputElements.jobLocation?.jobId == null -> originKeyWithNamespace

                !namespace.isNullOrBlank() ->
                    "jobs.${lastExistingOutputElements.jobLocation?.jobId}.steps." +
                        "$namespace.outputs.$originKeyWithNamespace"

                lastExistingOutputElements.stepAtom?.id.isNullOrBlank() -> originKeyWithNamespace

                else ->
                    "jobs.${lastExistingOutputElements.jobLocation?.jobId}.steps." +
                        "${lastExistingOutputElements.stepAtom?.id}.outputs.$originKeyWithNamespace"
            }
        } else if (!ciName.isNullOrBlank()) {
            ciName
        } else if (context.variables[nameAndValue.key] != null) {
            "variables.${nameAndValue.key}"
        } else "${nameAndValue.key}"
    }

    fun parseNameAndValueWithOr(
        context: PipelineExportContext,
        nameAndValueList: List<NameAndValue>? = emptyList(),
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?
    ): String {
        var ifString = ""
        nameAndValueList?.forEachIndexed { index, nameAndValue ->
            val preStr = parseNameAndValueWithPreStr(
                context = context,
                nameAndValue = nameAndValue,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
            )
            ifString += if (index == nameAndValueList.size - 1) {
                "$preStr != '${nameAndValue.value}'"
            } else {
                "$preStr != '${nameAndValue.value}' || "
            }
        }
        return ifString
    }

    fun parseStepIfFiled(
        context: PipelineExportContext,
        step: Element,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?
    ): String? {
        return when (step.additionalOptions?.runCondition) {
            RunCondition.CUSTOM_CONDITION_MATCH -> step.additionalOptions?.customCondition
            RunCondition.CUSTOM_VARIABLE_MATCH -> {
                val ifString = parseNameAndValueWithAnd(
                    context = context,
                    nameAndValueList = step.additionalOptions?.customVariables,
                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                )
                if (step.additionalOptions?.customVariables?.isEmpty() == true) null
                else ifString
            }
            RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                val ifString = parseNameAndValueWithOr(
                    context = context,
                    nameAndValueList = step.additionalOptions?.customVariables,
                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                )
                if (step.additionalOptions?.customVariables?.isEmpty() == true) null
                else ifString
            }
            RunCondition.PRE_TASK_FAILED_BUT_CANCEL ->
                IfType.ALWAYS_UNLESS_CANCELLED.name
            RunCondition.PRE_TASK_FAILED_EVEN_CANCEL ->
                IfType.ALWAYS.name
            RunCondition.PRE_TASK_FAILED_ONLY ->
                IfType.FAILURE.name
            else -> null
        }
    }

    fun replaceMapWithDoubleCurlyBraces(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        inputMap: MutableMap<String, Any>?,
        relyMap: Map<String, Any>? = null,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): Map<String, Any?>? {
        if (inputMap.isNullOrEmpty()) {
            return null
        }
        val result = mutableMapOf<String, Any>()
        inputMap.forEach lit@{ (key, value) ->
            val rely = relyMap?.get(key) as Map<String, Any>?
            if (rely.isNullOrEmpty()) {
                result[key] = replaceValueWithDoubleCurlyBraces(
                    allInfo = allInfo,
                    context = context,
                    value = value,
                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                )
                return@lit
            }

            if (rely["expression"] == null) {
                return@lit
            }

            try {
                val expression = rely["expression"] as List<Map<String, Any>>
                when (rely["operation"]) {
                    "AND" -> {
                        expression.forEach {
                            if (checkRely(inputMap[it["key"]], it["value"], it["regex"])) {
                                result[key] = replaceValueWithDoubleCurlyBraces(
                                    allInfo = allInfo,
                                    context = context,
                                    value = value,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                )
                                return@lit
                            }
                        }
                    }
                    "OR" -> {
                        expression.forEach {
                            if (checkRely(inputMap[it["key"]], it["value"], it["regex"])) {
                                result[key] = replaceValueWithDoubleCurlyBraces(
                                    allInfo = allInfo,
                                    context = context,
                                    value = value,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                )
                                return@lit
                            }
                        }
                        return@lit
                    }
                }
            } catch (e: Exception) {
                logger.warn("load atom input[rely] with error: ${e.message} ,rely=$rely")
            }
        }
        return result
    }

    private fun checkRely(key: Any?, value: Any?, regex: Any?): Boolean {
        if (value != null) return key == value
        if (regex != null) return key.toString().contains(Regex(regex.toString()))
        return false
    }

    private fun replaceValueWithDoubleCurlyBraces(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        value: Any,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): Any {
        if (value is String) {
            return ExportStepRun.replaceStringWithDoubleCurlyBraces(
                allInfo = allInfo,
                context = context,
                value = value,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
            )
        }
        if (value is List<*>) {
            val result = mutableListOf<Any?>()
            value.forEach {
                if (it is String) {
                    result.add(
                        ExportStepRun.replaceStringWithDoubleCurlyBraces(
                            allInfo = allInfo,
                            context = context,
                            value = it,
                            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                        )
                    )
                } else {
                    result.add(it)
                }
            }
            return result
        }

        return value
    }
}
