package com.tencent.devops.process.service.pipelineExport

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessCode.BK_FIND_RECOMMENDED_REPLACEMENT_PLUG
import com.tencent.devops.process.constant.ProcessCode.BK_OLD_PLUG_NOT_SUPPORT
import com.tencent.devops.process.constant.ProcessCode.BK_PLEASE_USE_STAGE_AUDIT
import com.tencent.devops.process.constant.ProcessCode.BK_PLUG_NOT_SUPPORTED
import com.tencent.devops.process.pojo.MarketBuildAtomElementWithLocation
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import org.slf4j.LoggerFactory

object ExportStep {

    private val logger = LoggerFactory.getLogger(ExportStep::class.java)

    private val checkoutAtomCodeSet = listOf(
        "gitCodeRepo",
        "gitCodeRepoCommon",
        "checkout"
    )

    @Suppress("ComplexMethod", "LongMethod", "NestedBlockDepth")
    fun getV2StepFromJob(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        job: Container,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): List<PreStep> {
        val stepList = mutableListOf<PreStep>()

        // 根据job里的elements统一查询数据库的store里的ATOM表prob字段
        val thirdPartyElementList = mutableListOf<ElementThirdPartySearchParam>()
        job.elements.forEach { element ->
            when (element.getClassType()) {
                MarketBuildAtomElement.classType -> {
                    val step = element as MarketBuildAtomElement
                    thirdPartyElementList.add(
                        ElementThirdPartySearchParam(
                            atomCode = step.getAtomCode(),
                            version = step.version
                        )
                    )
                }
                MarketBuildLessAtomElement.classType -> {
                    val step = element as MarketBuildLessAtomElement
                    thirdPartyElementList.add(
                        ElementThirdPartySearchParam(
                            atomCode = step.getAtomCode(),
                            version = step.version
                        )
                    )
                }
                else -> {
                }
            }
        }
        val relyList = allInfo.getAtomRely(GetRelyAtom(thirdPartyElementList))
        logger.info("[${allInfo.pipelineInfo.projectId}] getV2StepFromJob export relyList: $relyList ")
        job.elements.forEach { element ->
            val originRetryTimes = element.additionalOptions?.retryCount ?: 0
            val originTimeout = element.additionalOptions?.timeout?.toInt() ?: 480
            val retryTimes = if (originRetryTimes > 1) originRetryTimes else null
            val timeoutMinutes = if (originTimeout < 480) originTimeout else null
            val continueOnError = if (element.additionalOptions?.continueWhenFailed == true) true else null
            pipelineExportV2YamlConflictMapItem.step =
                PipelineExportV2YamlConflictMapBaseItem(
                    id = element.id,
                    name = element.name
                )
            when (element.getClassType()) {
                // Bash脚本插件直接转为run
                LinuxScriptElement.classType -> {
                    val step = element as LinuxScriptElement
                    stepList.add(
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // bat插件上的
                            ifFiled = ExportCondition.parseStepIfFiled(
                                context = context,
                                step = step,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            uses = null,
                            with = null,
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = ExportStepRun.formatScriptOutput(
                                allInfo = allInfo,
                                context = context,
                                script = step.script,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            checkout = null,
                            shell = null
                        )
                    )
                }
                WindowsScriptElement.classType -> {
                    val step = element as WindowsScriptElement
                    stepList.add(
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // bat插件上的
                            ifFiled = ExportCondition.parseStepIfFiled(
                                context = context,
                                step = step,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            uses = null,
                            with = null,
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = ExportStepRun.formatScriptOutput(
                                allInfo = allInfo,
                                context = context,
                                script = step.script,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            checkout = null,
                            shell = null
                        )
                    )
                }
                MarketBuildAtomElement.classType -> {
                    val step = element as MarketBuildAtomElement
                    val input = element.data["input"]
                    val output = element.data["output"]
                    val namespace = element.data["namespace"] as String?
                    val inputMap = if (input != null && (input as MutableMap<String, Any>).isNotEmpty()) {
                        input
                    } else null
                    logger.info(
                        "[${allInfo.pipelineInfo.projectId}] getV2StepFromJob export MarketBuildAtom " +
                            "atomCode(${step.getAtomCode()}), inputMap=$inputMap, step=$step"
                    )
                    if (output != null && (output as MutableMap<String, Any>).isNotEmpty()) {
                        output.keys.forEach { key ->
                            val outputWithNamespace = if (namespace.isNullOrBlank()) key else "${namespace}_$key"
                            val conflictElements = context.output2Elements[outputWithNamespace]
                            val item = MarketBuildAtomElementWithLocation(
                                stageLocation = pipelineExportV2YamlConflictMapItem.stage?.copy(),
                                jobLocation = pipelineExportV2YamlConflictMapItem.job?.copy(),
                                stepAtom = step
                            )
                            if (!conflictElements.isNullOrEmpty()) {
                                conflictElements.add(item)
                            } else {
                                context.output2Elements[outputWithNamespace] = mutableListOf(item)
                            }
                        }
                    }
                    var thisIsHandsomeAtom = false
                    thisIsHandsomeAtom = ExportStepCheckout.addCheckoutAtom(
                        allInfo = allInfo,
                        context = context,
                        stepList = stepList,
                        atomCode = step.getAtomCode(),
                        step = step,
                        inputMap = inputMap,
                        timeoutMinutes = timeoutMinutes,
                        continueOnError = continueOnError,
                        retryTimes = retryTimes,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    ) || thisIsHandsomeAtom
                    thisIsHandsomeAtom = ExportStepRun.addRunAtom(
                        allInfo = allInfo,
                        context = context,
                        stepList = stepList,
                        atomCode = step.getAtomCode(),
                        step = step,
                        inputMap = inputMap,
                        timeoutMinutes = timeoutMinutes,
                        continueOnError = continueOnError,
                        retryTimes = retryTimes,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    ) || thisIsHandsomeAtom
                    if (!thisIsHandsomeAtom) stepList.add(
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // 插件上的
                            ifFiled = ExportCondition.parseStepIfFiled(
                                context = context,
                                step = step,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            uses = "${step.getAtomCode()}@${step.version}",
                            with = ExportCondition.replaceMapWithDoubleCurlyBraces(
                                allInfo = allInfo,
                                context = context,
                                inputMap = inputMap,
                                relyMap = relyList?.get(step.getAtomCode()),
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
                        )
                    )
                }
                MarketBuildLessAtomElement.classType -> {
                    val step = element as MarketBuildLessAtomElement
                    val input = element.data["input"]
                    val inputMap = if (input != null && !(input as MutableMap<String, Any>).isNullOrEmpty()) {
                        input
                    } else null
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
                            uses = "${step.getAtomCode()}@${step.version}",
                            with = ExportCondition.replaceMapWithDoubleCurlyBraces(
                                allInfo = allInfo,
                                context = context,
                                inputMap = inputMap,
                                relyMap = relyList?.get(step.getAtomCode()),
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                            ),
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
                        )
                    )
                }
                ManualReviewUserTaskElement.classType -> {
                    val step = element as ManualReviewUserTaskElement
                    stepList.add(
                        PreStep(
                            name = null,
                            id = step.stepId,
                            ifFiled = null,
                            uses = "### [${step.name}] " + MessageUtil.getMessageByLocale(
                                messageCode = BK_PLEASE_USE_STAGE_AUDIT,
                                language = I18nUtil.getLanguage()
                            ),
                            with = null,
                            timeoutMinutes = null,
                            continueOnError = null,
                            retryTimes = null,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
                        )
                    )
                }
                else -> {
                    logger.info("Not support plugin:${element.getClassType()}, skip...")
                    context.yamlSb.append(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_PLUG_NOT_SUPPORTED,
                            language = I18nUtil.getLanguage(),
                            params = arrayOf(element.name, element.getClassType())
                        ) + "," + MessageUtil.getMessageByLocale(
                            messageCode = BK_FIND_RECOMMENDED_REPLACEMENT_PLUG,
                            language = I18nUtil.getLanguage()
                        ) + "\n"
                    )
                    stepList.add(
                        PreStep(
                            name = null,
                            id = element.stepId,
                            ifFiled = null,
                            uses = "### [${element.name}] " + MessageUtil.getMessageByLocale(
                                messageCode = BK_OLD_PLUG_NOT_SUPPORT,
                                language = I18nUtil.getLanguage()
                            ),
                            with = null,
                            timeoutMinutes = null,
                            continueOnError = null,
                            retryTimes = null,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
                        )
                    )
                }
            }
        }
        return stepList
    }
}
