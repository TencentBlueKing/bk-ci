package com.tencent.devops.process.service.pipelineExport

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.service.pipelineExport.pojo.CheckoutAtomParam
import com.tencent.devops.process.service.pipelineExport.pojo.GitCodeRepoAtomParam
import com.tencent.devops.process.service.pipelineExport.pojo.GitCodeRepoCommonAtomParam
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import org.slf4j.LoggerFactory

object ExportStepCheckout {

    private val logger = LoggerFactory.getLogger(ExportStepCheckout::class.java)

    private const val LIMIT_MESSAGE = "[该字段限制导出，请手动填写]"
    private val checkoutAtomCodeSet = listOf(
        "gitCodeRepo",
        "gitCodeRepoCommon",
        "checkout"
    )

    private fun parseGitCodeRepo(
        allInfo: PipelineExportInfo,
        inputMap: MutableMap<String, Any>?
    ): MutableMap<String, Any> {
        val input = kotlin.runCatching { JsonUtil.anyTo(inputMap, object : TypeReference<GitCodeRepoAtomParam>() {}) }
            .getOrElse {
                logger.warn("parseGitCodeRepo input error $inputMap", it)
                return mutableMapOf()
            }
        val repo = allInfo.getRepoInfo(allInfo.pipelineInfo.projectId, input.getRepositoryConfig())
        val url = repo?.url ?: input.repositoryName?.ifBlank { null } ?: LIMIT_MESSAGE

        val toCheckoutAtom = CheckoutAtomParam(input)

        val fixInputMap =
            kotlin.runCatching { JsonUtil.anyTo(toCheckoutAtom, object : TypeReference<Map<String, Any>>() {}) }
                .getOrElse {
                    logger.warn("parseGitCodeRepo fixInputMap error $inputMap", it)
                    return mutableMapOf()
                }.toMutableMap()

        fixInputMap["repositoryUrl"] = url

        return fixInputMap
    }

    private fun MutableMap<String, Any>.updateIfNotAbsent(key: String, value: String) {
        val res = get(key)
        if (res is String && res.isNotBlank()) {
            put(key, value)
        }
    }

    private fun parseGitCodeRepoCommon(
        allInfo: PipelineExportInfo,
        inputMap: MutableMap<String, Any>?
    ): MutableMap<String, Any> {
        val input =
            kotlin.runCatching { JsonUtil.anyTo(inputMap, object : TypeReference<GitCodeRepoCommonAtomParam>() {}) }
                .getOrElse {
                    logger.warn("parseGitCodeRepoCommon input error $inputMap", it)
                    return mutableMapOf()
                }
        val url = input.repositoryUrl ?: ""

        val toCheckoutAtom = CheckoutAtomParam(input)

        val fixInputMap =
            kotlin.runCatching { JsonUtil.anyTo(toCheckoutAtom, object : TypeReference<Map<String, Any>>() {}) }
                .getOrElse {
                    logger.warn("parseGitCodeRepoCommon fixInputMap error $inputMap", it)
                    return mutableMapOf()
                }.toMutableMap()

        fixInputMap["repositoryUrl"] = url

        return fixInputMap
    }

    private fun parseCheckout(
        allInfo: PipelineExportInfo,
        inputMap: MutableMap<String, Any>?
    ): MutableMap<String, Any> {
        val input = kotlin.runCatching { JsonUtil.anyTo(inputMap, object : TypeReference<CheckoutAtomParam>() {}) }
            .getOrElse {
                logger.warn("parseGitCodeRepo input error $inputMap", it)
                return mutableMapOf()
            }
        val url = if (input.repositoryType == CheckoutAtomParam.CheckoutRepositoryType.URL) {
            input.repositoryUrl ?: ""
        } else {
            val repo = allInfo.getRepoInfo(allInfo.pipelineInfo.projectId, input.getRepositoryConfig())
            repo?.url ?: input.repositoryName?.ifBlank { null } ?: LIMIT_MESSAGE
        }

        val fixInputMap =
            kotlin.runCatching { JsonUtil.anyTo(input, object : TypeReference<Map<String, Any>>() {}) }
                .getOrElse {
                    logger.warn("parseGitCodeRepo fixInputMap error $inputMap", it)
                    return mutableMapOf()
                }.toMutableMap()

        fixInputMap["repositoryUrl"] = url

        return fixInputMap
    }

    fun addCheckoutAtom(
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
        if (inputMap == null || atomCode.isBlank() || !checkoutAtomCodeSet.contains(atomCode)) return false
        logger.info(
            "[${allInfo.pipelineInfo.projectId}] " +
                "addCheckoutAtom export with atomCode($atomCode), inputMap=$inputMap, step=$step"
        )
        try {
            val fixInputMap = when (atomCode) {
                "gitCodeRepo" -> parseGitCodeRepo(allInfo, inputMap)
                "gitCodeRepoCommon" -> parseGitCodeRepoCommon(allInfo, inputMap)
                "checkout" -> parseCheckout(allInfo, inputMap)
                else -> mutableMapOf()
            }
            val url = fixInputMap["repositoryUrl"] as String?

            // 去掉所有插件上的凭证配置
            fixInputMap.updateIfNotAbsent("credentialId", LIMIT_MESSAGE)
            fixInputMap.updateIfNotAbsent("ticketId", LIMIT_MESSAGE)
            fixInputMap.updateIfNotAbsent("username", LIMIT_MESSAGE)
            fixInputMap.updateIfNotAbsent("password", LIMIT_MESSAGE)
            fixInputMap.updateIfNotAbsent("username", LIMIT_MESSAGE)
            fixInputMap.updateIfNotAbsent("accessToken", LIMIT_MESSAGE)
            fixInputMap.updateIfNotAbsent("personalAccessToken", LIMIT_MESSAGE)

            // 去掉原来的仓库指定参数
            fixInputMap.remove("repositoryType")
            fixInputMap.remove("repositoryHashId")
            fixInputMap.remove("repositoryName")
            fixInputMap.remove("repositoryUrl")
            fixInputMap.remove("authType")

            val relyMap = allInfo.getAtomRely(
                GetRelyAtom(
                    listOf(
                        ElementThirdPartySearchParam(
                            atomCode = "checkout",
                            version = "1.*"
                        )
                    )
                )
            )?.get("checkout")

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
                    with = ExportCondition.replaceMapWithDoubleCurlyBraces(
                        allInfo = allInfo,
                        context = context,
                        inputMap = fixInputMap,
                        relyMap = relyMap,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    ),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = null,
                    checkout = ExportStepRun.replaceStringWithDoubleCurlyBraces(
                        allInfo = allInfo,
                        context = context,
                        value = url ?: "",
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                    ),
                    shell = null
                )
            )
            return true
        } catch (e: Exception) {
            logger.error("[${allInfo.pipelineInfo.projectId}] addCheckoutAtom failed to convert atom[$atomCode]: ", e)
        }
        return false
    }
}
