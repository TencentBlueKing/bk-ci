package com.tencent.devops.process.service.pipelineExport

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.MarketBuildAtomElementWithLocation
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.service.pipelineExport.pojo.RunAtomParam
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import javax.ws.rs.core.Response

object ExportStepRun {
    private val logger = LoggerFactory.getLogger(ExportStepRun::class.java)
    private const val RUN_ATOM_NAME = "run"

    fun addRunAtom(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        stepList: MutableList<PreStep>,
        atomCode: String,
        step: MarketBuildAtomElement,
        inputMap: MutableMap<String, Any>?,
        timeoutMinutes: Int?,
        continueOnError: Boolean?,
        retryTimes: Int?,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): Boolean {
        if (inputMap == null || atomCode.isBlank() || !RUN_ATOM_NAME.contains(atomCode)) return false
        logger.info(
            "[${allInfo.pipelineInfo.projectId}] " +
                "addCheckoutAtom export with atomCode($atomCode), inputMap=$inputMap, step=$step"
        )
        try {
            val toRunAtom = kotlin.runCatching { JsonUtil.anyTo(inputMap, object : TypeReference<RunAtomParam>() {}) }
                .getOrElse {
                    logger.warn("parseRunAtom input error $inputMap", it)
                    return false
                }

            stepList.add(
                PreStep(
                    name = step.name,
                    id = step.stepId,
                    // 插件上的
                    ifFiled = ExportCondition.parseStepIfFiled(
                        context = context,
                        step = step,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    ),
                    uses = null,
                    with = toRunAtom.getWith().ifEmpty { null },
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = formatScriptOutput(
                        allInfo = allInfo,
                        context = context,
                        script = toRunAtom.script ?: "",
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    ),
                    checkout = null,
                    shell = toRunAtom.shell
                )
            )
            return true
        } catch (e: Exception) {
            logger.error("[${allInfo.pipelineInfo.projectId}] addCheckoutAtom failed to convert atom[$atomCode]: ", e)
        }
        return false
    }

    fun formatScriptOutput(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        script: String,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): String {
        return replaceStringWithDoubleCurlyBraces(
            allInfo = allInfo,
            context = context,
            value = parseSetEnv(script),
            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
        )
    }

    fun parseSetEnv(script: String): String {
        val regex = Regex("setEnv\\s+([^\\n]*)")
        val foundMatches = regex.findAll(script)
        var formatScript: String = script
        foundMatches.forEach { result ->
            val keyValueStr = if (result.groupValues.size >= 2) result.groupValues[1] else return@forEach
            val keyAndValue = keyValueStr.split(Regex("\\s+"))
            if (keyAndValue.size < 2) return@forEach
            val key = keyAndValue[0].removeSurrounding("\"")
            val value = keyValueStr.removePrefix(keyAndValue[0]).trim().removeSurrounding("\"")
            formatScript =
                formatScript.replace(result.value, "echo \"::set-output name=$key::$value\"")
        }
        return formatScript
    }

    @Suppress("NestedBlockDepth")
    fun replaceStringWithDoubleCurlyBraces(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        value: String,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): String {
        val pattern = Pattern.compile("\\\$\\{\\{?([^{}]+?)}?}")
        val matcher = pattern.matcher(value)
        var newValue = value
        while (matcher.find()) {
            val originKey = matcher.group(1).trim()
            // 假设匹配到了前序插件的output则优先引用，否则引用全局变量
            val existingOutputElements = context.output2Elements[originKey]
            var lastExistingOutputElements = MarketBuildAtomElementWithLocation()
            run outside@{
                existingOutputElements?.reversed()?.forEach {
                    if (it.jobLocation?.id == pipelineExportV2YamlConflictMapItem.job?.id ||
                        it.stageLocation?.id != pipelineExportV2YamlConflictMapItem.stage?.id
                    ) {
                        lastExistingOutputElements = it
                        return@outside
                    }
                }
            }
            val ciName = PipelineVarUtil.fetchReverseVarName(originKey)
            val namespace = lastExistingOutputElements.stepAtom?.data?.get("namespace") as String?
            val originKeyWithNamespace = if (!namespace.isNullOrBlank()) {
                originKey.replace("${namespace}_", "")
            } else originKey

            val realValue = when {
                lastExistingOutputElements.stepAtom != null &&
                    lastExistingOutputElements.jobLocation?.jobId != null -> {
                    checkConflictOutput(
                        allInfo = allInfo,
                        context = context,
                        key = originKey,
                        existingOutputElements = existingOutputElements!!,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    )
                    "\${{ jobs.${lastExistingOutputElements.jobLocation?.jobId}.steps." +
                        "${namespace?.ifBlank { null } ?: lastExistingOutputElements.stepAtom?.id}" +
                        ".outputs.$originKeyWithNamespace }}"
                }
                context.variables[originKey] != null -> "\${{ variables.$originKeyWithNamespace }}"
                !ciName.isNullOrBlank() -> "\${{ $ciName }}"
                else -> "\${{ $originKeyWithNamespace }}"
            }
            newValue = newValue.replace(matcher.group(), realValue)
        }
        return removeEndWhitespace(newValue)
    }

    private fun removeEndWhitespace(value: String): String {
        return value.split("\n").asSequence()
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n") {
                // 替换制表符，不然导出yaml格式会乱
                it.replace("\t", "    ").trimEnd()
            }
    }

    private fun checkConflictOutput(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        key: String,
        existingOutputElements: MutableList<MarketBuildAtomElementWithLocation>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ) {
        val distinctMap = HashMap<String?, MarketBuildAtomElementWithLocation>()
        existingOutputElements.forEach {
            distinctMap[it.jobLocation?.id] = it
        }
        val realExistingOutputElements =
            distinctMap.values.groupBy { it.stageLocation?.id }
        realExistingOutputElements.keys.reversed().forEach {
            if (it == pipelineExportV2YamlConflictMapItem.stage?.id ||
                realExistingOutputElements[it]?.size!! < 2
            ) return
            val names = realExistingOutputElements[it]?.map { _it -> _it.stepAtom?.name }
            val conflictElements = context.outputConflictMap[key]
            val itemElements = realExistingOutputElements[it]?.map { _it ->
                PipelineExportV2YamlConflictMapItem(
                    stage = _it.stageLocation?.copy(),
                    job = _it.jobLocation?.copy(),
                    step = PipelineExportV2YamlConflictMapBaseItem(
                        id = _it.stepAtom?.id,
                        name = _it.stepAtom?.name
                    )
                )
            } ?: return@forEach
            val item = mutableListOf(
                PipelineExportV2YamlConflictMapItem(
                    stage = pipelineExportV2YamlConflictMapItem.stage?.copy(),
                    job = pipelineExportV2YamlConflictMapItem.job?.copy(),
                    step = pipelineExportV2YamlConflictMapItem.step?.copy()
                )
            )
            item.addAll(itemElements)
            if (!conflictElements.isNullOrEmpty()) {
                conflictElements.add(item.toList())
            } else {
                context.outputConflictMap[key] = mutableListOf(item.toList())
            }
            if (allInfo.exportFile) {
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = ProcessMessageCode.ERROR_EXPORT_OUTPUT_CONFLICT,
                    params = arrayOf(key, names.toString())
                )
            }
            return
        }
    }
}
