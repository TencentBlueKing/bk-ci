/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_VIEW_DETAILS
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.quality.pojo.QualityRuleInterceptRecord
import com.tencent.devops.common.quality.pojo.RuleCheckResult
import com.tencent.devops.common.quality.pojo.RuleCheckSingleResult
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.PIPELINE_QUALITY_AUDIT_NOTIFY_TEMPLATE_V2
import com.tencent.devops.notify.PIPELINE_QUALITY_END_NOTIFY_TEMPLATE_V2
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.quality.api.v2.pojo.QualityHisMetadata
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.response.AtomRuleResponse
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import com.tencent.devops.quality.bean.QualityUrlBean
import com.tencent.devops.quality.constant.BK_BLOCKED
import com.tencent.devops.quality.constant.BK_BUILD_INTERCEPTED_TERMINATED
import com.tencent.devops.quality.constant.BK_BUILD_INTERCEPTED_TO_BE_REVIEWED
import com.tencent.devops.quality.constant.BK_CURRENT_VALUE
import com.tencent.devops.quality.constant.BK_INTERCEPTION_METRICS
import com.tencent.devops.quality.constant.BK_INTERCEPTION_RULES
import com.tencent.devops.quality.constant.BK_NO_TOOL_OR_RULE_ENABLED
import com.tencent.devops.quality.constant.BK_PASSED
import com.tencent.devops.quality.constant.DEFAULT_CODECC_URL
import com.tencent.devops.quality.constant.codeccToolUrlPathMap
import com.tencent.devops.quality.pojo.RefreshType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import com.tencent.devops.quality.service.QualityNotifyGroupService
import com.tencent.devops.quality.util.ElementUtils
import com.tencent.devops.quality.util.ThresholdOperationUtil
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.concurrent.Executors
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "NestedBlockDepth",
    "ReturnCount",
    "MagicNumber",
    "ComplexMethod",
    "LongMethod",
    "LargeClass"
)
class QualityRuleCheckService @Autowired constructor(
    private val ruleService: QualityRuleService,
    private val qualityHisMetadataService: QualityHisMetadataService,
    private val qualityNotifyGroupService: QualityNotifyGroupService,
    private val countService: QualityCountService,
    private val historyService: QualityHistoryService,
    private val controlPointService: QualityControlPointService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val qualityCacheService: QualityCacheService,
    private val qualityRuleBuildHisService: QualityRuleBuildHisService,
    private val qualityUrlBean: QualityUrlBean
) {
    private val executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun userGetMatchRuleList(projectId: String, pipelineId: String): List<QualityRuleMatchTask> {
        // 取出项目下包含该流水线的所有红线，再按控制点分组
        val filterRuleList = ruleService.getProjectRuleList(projectId, pipelineId, null)
        return ruleService.listMatchTask(filterRuleList)
    }

    fun userGetMatchTemplateList(projectId: String, templateId: String?): List<QualityRuleMatchTask> {
        val ruleList = ruleService.getProjectRuleList(projectId, null, templateId)
        return ruleService.listMatchTask(ruleList)
    }

    fun getMatchRuleListByCache(projectId: String, pipelineId: String): List<QualityRuleMatchTask> {
        val cacheData = qualityCacheService.getCacheRuleListByPipelineId(projectId, pipelineId)
        if (cacheData != null) {
            return cacheData
        }
        logger.info("userGetMatchRuleList redis is empty, $projectId| $pipelineId")
        // 取出项目下包含该流水线的所有红线，再按控制点分组
        val qualityTasks = userGetMatchRuleList(projectId, pipelineId)
        qualityCacheService.refreshCache(
            projectId = projectId,
            pipelineId = pipelineId,
            templateId = null,
            ruleTasks = qualityTasks,
            type = RefreshType.GET
        )
        return qualityTasks
    }

    fun getMatchTemplateListByCache(projectId: String, templateId: String?): List<QualityRuleMatchTask> {
        if (templateId.isNullOrBlank()) return listOf()
        val cacheData = qualityCacheService.getCacheRuleListByTemplateId(projectId, templateId)
        if (cacheData != null) {
            return cacheData
        }
        logger.info("userGetMatchTemplateList redis is empty, $projectId| $templateId")
        val qualityTasks = userGetMatchTemplateList(projectId, templateId)
        qualityCacheService.refreshCache(
            projectId = projectId,
            pipelineId = null,
            templateId = templateId,
            ruleTasks = qualityTasks,
            type = RefreshType.GET
        )
        return qualityTasks
    }

    fun userListAtomRule(
        projectId: String,
        pipelineId: String,
        atomCode: String,
        atomVersion: String
    ): AtomRuleResponse {
        val filterRuleList = ruleService.getProjectRuleList(
            projectId = projectId,
            pipelineId = pipelineId,
            templateId = null
        ).filter { it.controlPoint.name == atomCode }
        val ruleList = ruleService.listMatchTask(filterRuleList)
        val isControlPoint = controlPointService.isControlPoint(atomCode, atomVersion, projectId)
        return AtomRuleResponse(isControlPoint, ruleList)
    }

    fun userListTemplateAtomRule(
        projectId: String,
        templateId: String,
        atomCode: String,
        atomVersion: String
    ): AtomRuleResponse {
        val filterRuleList = ruleService.getProjectRuleList(
            projectId = projectId,
            pipelineId = null,
            templateId = templateId
        ).filter { it.controlPoint.name == atomCode }
        val ruleList = ruleService.listMatchTask(filterRuleList)
        val isControlPoint = controlPointService.isControlPoint(atomCode, atomVersion, projectId)
        return AtomRuleResponse(isControlPoint, ruleList)
    }

    fun check(buildCheckParams: BuildCheckParams): RuleCheckResult {
        val pipelineId = buildCheckParams.pipelineId
        val templateId = buildCheckParams.templateId
        val ruleList = mutableSetOf<QualityRule>()
        val watcher = Watcher(id = "QUALITY|check|${buildCheckParams.projectId}|" +
                "${buildCheckParams.buildId}|$templateId")
        try {
            watcher.start("listPipelineRange")
            // 匹配拦截规则
            if (!pipelineId.isNullOrBlank()) {
                ruleList.addAll(
                    ruleService.serviceListByPipelineRange(
                        buildCheckParams.projectId,
                        buildCheckParams.pipelineId
                    ).filter { it.controlPoint.position.name == buildCheckParams.position }
                )
            }
            watcher.stop()
            watcher.start("listTemplateRange")
            if (!templateId.isNullOrBlank()) {
                ruleList.addAll(
                    ruleService.serviceListByTemplateRange(
                        buildCheckParams.projectId,
                        buildCheckParams.templateId
                    ).filter { it.controlPoint.position.name == buildCheckParams.position }
                )
            }
            watcher.stop()
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 500, errorThreshold = 3000)
        }

        return doCheckRules(buildCheckParams, ruleList.toList())
    }

    fun checkBuildHis(buildCheckParams: BuildCheckParamsV3): RuleCheckResult {
        val ruleBuildId = buildCheckParams.ruleBuildIds.map {
            HashUtil.decodeIdToLong(it)
        }

        // 遍历项目下所有拦截规则
        val ruleList = qualityRuleBuildHisService.list(ruleBuildId)

        // 更新build id
        qualityRuleBuildHisService.updateBuildId(ruleBuildId, buildCheckParams.buildId)

        // 更新gateKeepers/ruleValue变量
        qualityRuleBuildHisService.convertVariables(ruleList, buildCheckParams)

        val params = BuildCheckParams(
            projectId = buildCheckParams.projectId,
            pipelineId = buildCheckParams.pipelineId,
            buildId = buildCheckParams.buildId,
            buildNo = "",
            interceptTaskName = buildCheckParams.interceptName ?: "",
            startTime = System.currentTimeMillis(),
            taskId = "",
            position = buildCheckParams.position,
            templateId = buildCheckParams.templateId,
            stageId = buildCheckParams.stageId ?: "",
            runtimeVariable = buildCheckParams.runtimeVariable
        )
        return doCheckRules(buildCheckParams = params, ruleList = ruleList)
    }

    private fun doCheckRules(buildCheckParams: BuildCheckParams, ruleList: List<QualityRule>): RuleCheckResult {
        with(buildCheckParams) {
            logger.info("QUALITY|doCheckRules buildCheckParams is|$buildCheckParams")
            val filterRuleList = ruleList.filter { rule ->
                logger.info("validate whether to check rule(${rule.name}) with gatewayId(${rule.gatewayId})")
                if (buildCheckParams.taskId.isNotBlank() && rule.controlPoint.name != buildCheckParams.taskId) {
                    return@filter false
                }
                val gatewayId = rule.gatewayId ?: ""
                if (!buildCheckParams.interceptTaskName.toLowerCase().contains(gatewayId.toLowerCase())) {
                    return@filter false
                }

                val containsInPipeline = rule.range.contains(pipelineId)
                val containsInTemplate = rule.templateRange.contains(buildCheckParams.templateId)
                return@filter (containsInPipeline || containsInTemplate)
            }
            logger.info("QUALITY|filterRuleList is: $filterRuleList")

            val resultPair = doCheck(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                filterRuleList = filterRuleList,
                runtimeVariable = runtimeVariable
            )
            val resultList = resultPair.first
            val ruleInterceptList = resultPair.second

            // 异步后续的处理
            executors.execute {
                checkPostHandle(buildCheckParams, ruleInterceptList, resultList)
            }

            // 记录结果
            val checkTimes = recordHistory(buildCheckParams, ruleInterceptList)

            return genResult(projectId, pipelineId, buildId, checkTimes, resultList, ruleInterceptList)
        }
    }

    private fun doCheck(
        projectId: String,
        pipelineId: String,
        buildId: String,
        filterRuleList: List<QualityRule>,
        runtimeVariable: Map<String, String>?
    ): Pair<List<RuleCheckSingleResult>, List<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>> {
        val resultList = mutableListOf<RuleCheckSingleResult>()
        val ruleInterceptList = mutableListOf<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>()

        // start to check
        val metadataList = qualityHisMetadataService.serviceGetHisMetadata(buildId)
        filterRuleList.forEach { rule ->
            logger.info("start to check rule(${rule.name})")

            val result = checkIndicator(
                rule.controlPoint.name,
                rule.indicators,
                metadataList,
                rule.taskSteps
            )
            val interceptRecordList = result.second
            val interceptResult = result.first
            val params = mapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId,
                CodeccUtils.BK_CI_CODECC_TASK_ID to
                        (runtimeVariable?.get(CodeccUtils.BK_CI_CODECC_TASK_ID) ?: "")
            )

            resultList.add(getRuleCheckSingleResult(rule.name, interceptRecordList, params))
            ruleInterceptList.add(Triple(rule, interceptResult, interceptRecordList))

            val status = if (interceptResult) {
                RuleInterceptResult.PASS.name
            } else {
                if (rule.gateKeepers.isNullOrEmpty()) {
                    RuleInterceptResult.FAIL.name
                } else {
                    RuleInterceptResult.WAIT.name
                }
            }
            qualityRuleBuildHisService.updateStatus(HashUtil.decodeIdToLong(rule.hashId), status)
        }

        return Pair(resultList, ruleInterceptList)
    }

    private fun genResult(
        projectId: String,
        pipelineId: String,
        buildId: String,
        checkTimes: Int,
        resultList: List<RuleCheckSingleResult>,
        ruleInterceptList: List<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>
    ): RuleCheckResult {
        // generate result
        val failRule = ruleInterceptList.filter { !it.second }.map { it.first }
        logger.info("QUALITY|failRule is: $failRule")
        val allPass = failRule.isEmpty()
        val allEnd = allPass || (!allPass && !failRule.any { it.operation == RuleOperation.AUDIT } &&
                failRule.all { it.gateKeepers.isNullOrEmpty() })
        val auditTimeOutMinutes = if (!allPass) {
            Collections.min(failRule.map { it.auditTimeoutMinutes ?: DEFAULT_TIMEOUT_MINUTES })
        } else DEFAULT_TIMEOUT_MINUTES
        logger.info("check result allPass($allPass) allEnd($allEnd) auditTimeoutMinutes($auditTimeOutMinutes)")
        logger.info("end check pipeline build: $projectId, $pipelineId, $buildId")
        return RuleCheckResult(allPass, allEnd, auditTimeOutMinutes * 60L, checkTimes, resultList)
    }

    private fun checkPostHandle(
        buildCheckParams: BuildCheckParams,
        result: List<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>,
        resultList: List<RuleCheckSingleResult>
    ) {
        result.forEach {
            val rule = it.first
            val ruleId = HashUtil.decodeIdToLong(rule.hashId)
            val interceptResult = it.second

            with(buildCheckParams) {
                ruleService.plusExecuteCount(ruleId)

                if (!interceptResult) {
                    ruleService.plusInterceptTimes(ruleId)

                    try {
                        if (rule.opList != null) {
                            logger.info("do op list action: $buildId, ${rule.name}")
                            rule.opList!!.forEach { ruleOp ->
                                val finalRule = qualityRuleBuildHisService.listRuleBuildHis(
                                    listOf(ruleId)).firstOrNull()
                                logger.info("finalRule is : $finalRule")
                                if (!rule.gateKeepers.isNullOrEmpty() &&
                                    finalRule?.status == RuleInterceptResult.WAIT) {
                                    doRuleOperation(
                                        this,
                                        resultList.filter { result -> result.ruleName == rule.name },
                                        QualityRule.RuleOp(
                                            operation = RuleOperation.AUDIT,
                                            notifyTypeList = ruleOp.notifyTypeList,
                                            notifyGroupList = ruleOp.notifyGroupList,
                                            notifyUserList = ruleOp.notifyUserList,
                                            auditUserList = rule.gateKeepers,
                                            auditTimeoutMinutes = ruleOp.auditTimeoutMinutes
                                        )
                                    )
                                } else {
                                    doRuleOperation(buildCheckParams, resultList, ruleOp)
                                }
                            }
                        } else {
                            logger.info("op list is empty for rule and build: $buildId, ${rule.name}")
                            doRuleOperation(
                                this,
                                resultList.filter { result -> result.ruleName == rule.name },
                                QualityRule.RuleOp(
                                    operation = rule.operation,
                                    notifyTypeList = rule.notifyTypeList,
                                    notifyGroupList = rule.notifyGroupList,
                                    notifyUserList = rule.notifyUserList,
                                    auditUserList = rule.auditUserList,
                                    auditTimeoutMinutes = rule.auditTimeoutMinutes
                                )
                            )
                        }
                    } catch (ignored: Throwable) {
                        logger.warn("QUALITY|checkPostHandle|send notification fail|$buildId|warn=${ignored.message}")
                    }
                }
                countService.countIntercept(projectId, pipelineId, ruleId, interceptResult)
            }
        }
    }

    private fun doRuleOperation(
        buildCheckParams: BuildCheckParams,
        resultList: List<RuleCheckSingleResult>,
        ruleOp: QualityRule.RuleOp
    ) {
        with(buildCheckParams) {
            val createTime = LocalDateTime.now()
            if (ruleOp.operation == RuleOperation.END) {
                sendEndNotification(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildNo = buildNo,
                    createTime = createTime,
                    resultList = resultList,
                    endNotifyTypeList = ruleOp.notifyTypeList ?: listOf(),
                    endNotifyGroupList = ruleOp.notifyGroupList ?: listOf(),
                    endNotifyUserList = (ruleOp.notifyUserList ?: listOf()).map { user ->
                        EnvUtils.parseEnv(user, runtimeVariable ?: mapOf())
                    },
                    position = buildCheckParams.position,
                    stageId = buildCheckParams.stageId,
                    runtimeVariable = buildCheckParams.runtimeVariable
                )
            } else {
                // val startUser = runtimeVariable?.get(PIPELINE_START_USER_ID) ?: ""
                sendAuditNotification(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildNo = buildNo,
                    createTime = createTime,
                    resultList = resultList,
                    auditNotifyUserList = (ruleOp.auditUserList
                        ?: listOf()).toSet().map { user ->
                        EnvUtils.parseEnv(user, runtimeVariable ?: mapOf())
                    },
                    position = buildCheckParams.position,
                    stageId = buildCheckParams.stageId,
                    runtimeVariable = buildCheckParams.runtimeVariable
                )
            }
        }
    }

    private fun checkIndicator(
        controlPointName: String,
        indicators: List<QualityIndicator>,
        metadataList: List<QualityHisMetadata>,
        ruleTaskSteps: List<QualityRule.RuleTask>?
    ): Pair<Boolean, MutableList<QualityRuleInterceptRecord>> {
        var allCheckResult = true
        val interceptList = mutableListOf<QualityRuleInterceptRecord>()
        var ruleTaskStepsCopy = ruleTaskSteps?.toMutableList()
        // 借助临时list,把红线指标添加的控制点前缀塞进要判断的指标taskName
        if (!ruleTaskStepsCopy.isNullOrEmpty()) {
            indicators.forEach { indicator ->
                val taskStep = ruleTaskStepsCopy.firstOrNull { it.indicatorEnName == indicator.enName }
                indicator.taskName = taskStep?.taskName
                if (taskStep != null) ruleTaskStepsCopy.remove(taskStep)
            }
        }

        logger.info("QUALITY|metadataList is: $metadataList, indicators is:$indicators")

        val (indicatorsCopy, metadataListCopy) = handleWithMultiIndicator(indicators, metadataList)

        logger.info("QUALITY|indicatorsCopy is:$indicatorsCopy")
        // 遍历每个指标
        indicatorsCopy.forEach { indicator ->
            val thresholdType = indicator.thresholdType
            var checkResult = true

            // 脚本原子的指标特殊处理：取指标英文名 = 基础数据名
            val filterMetadataList = if (indicator.taskName.isNullOrBlank()) {
                if (indicator.isScriptElementIndicator()) {
                    listOf(metadataListCopy
                        .find { indicator.enName == it.enName &&
                                it.elementType in QualityIndicator.SCRIPT_ELEMENT })
                } else {
                    indicator.metadataList.map { metadata ->
                        metadataListCopy.find {
                            it.enName == metadata.enName && it.taskName.startsWith(indicator.taskName ?: "")
                        }
                    }.toList()
                }
            } else {
                if (indicator.isScriptElementIndicator()) {
                    listOf(
                        metadataListCopy.filter { it.elementType in QualityIndicator.SCRIPT_ELEMENT }
                        .find {
                            indicator.enName == it.enName &&
                                    it.taskName.startsWith(indicator.taskName ?: "")
                        }
                    )
                } else {
                    metadataListCopy.filter {
                        it.taskName.startsWith(indicator.taskName ?: "") &&
                        indicator.metadataList.map { metadata -> metadata.enName }.contains(it.enName)
                    }
                }
            }

            logger.info("QUALITY|filterMetadataList is:$filterMetadataList")

            // 遍历所有基础数据
            var elementDetail = ""
            val result: String? = when (thresholdType) {
                // int类型把所有基础数据累加
                QualityDataType.INT -> {
                    var result: Int? = null
                    for (it in filterMetadataList) {
                        // -1表示直接失败
                        if (DETAIL_NOT_RUN_VALUE == it?.value) {
                            result = null
                            break
                        }

                        if (it?.value != null && NumberUtils.isDigits(it.value)) {
                            val value = it.value.toInt()
                            result = (result ?: 0) + value
                            // 记录”查看详情“里面跳转的基础数据, 记录第一个
                            if (value >= 0 && elementDetail.isBlank()) elementDetail = it.detail
                        }
                    }
                    if (!ThresholdOperationUtil.valid(result?.toString(), indicator.threshold, indicator.operation)) {
                        checkResult = false
                        allCheckResult = false
                    }
                    result?.toString()
                }
                // float类型把所有基础数据累加
                QualityDataType.FLOAT -> {
                    var result: BigDecimal? = null
                    for (it in filterMetadataList) {

                        if (it?.value != null && NumberUtils.isCreatable(it.value)) {
                            val value = BigDecimal(it.value)

                            // -1表示直接失败
                            if (DETAIL_NOT_RUN_FLOAT_VALUE.compareTo(value) == 0) {
                                result = null
                                break
                            }

                            result = result?.plus(BigDecimal(it.value)) ?: BigDecimal(it.value)
                            // 记录”查看详情“里面跳转的基础数据
                            if (value >= BigDecimal(0) && elementDetail.isBlank()) elementDetail = it.detail
                        }
                    }
                    if (!ThresholdOperationUtil.validDecimal(
                            actualValue = result,
                            boundaryValue = BigDecimal(indicator.threshold),
                            operation = indicator.operation
                        )
                    ) {
                        checkResult = false
                        allCheckResult = false
                    }
                    result?.toString()
                }
                // 布尔类型把所有基础数据求与
                QualityDataType.BOOLEAN -> {
                    var result: Boolean? = null
                    val threshold = indicator.threshold.toBoolean()
                    logger.info("boolean threshold: $threshold")
                    for (it in filterMetadataList) {
                        logger.info("each value: ${it?.value}")
                        if (it?.value != null &&
                            (it.value.toLowerCase() == "true" || it.value.toLowerCase() == "false")
                        ) {
                            val value = it.value.toBoolean()
                            logger.info("each convert value: $value")
                            if (value != threshold) {
                                checkResult = false
                                allCheckResult = false
                                result = value
                                // 记录”查看详情“里面跳转的基础数据
                                elementDetail = it.detail
                                break
                            } else {
                                // 全通过了，也要有值
                                result = threshold
                            }
                        }
                    }

                    // 全为null，不通过
                    if (!ThresholdOperationUtil.validBoolean(
                            result?.toString()
                                ?: "", indicator.threshold, indicator.operation
                        )
                    ) {
                        checkResult = false
                        allCheckResult = false
                    }
                    result?.toString()
                }
                else -> {
                    null
                }
            }
            with(indicator) {
                interceptList.add(
                    QualityRuleInterceptRecord(
                        indicatorId = hashId,
                        indicatorName = if (indicator.taskName.isNullOrEmpty()) {
                            cnName
                        } else {
                            "[${indicator.taskName!!.substringBeforeLast("+")}]$cnName"
                        },
                        indicatorType = elementType,
                        controlPoint = controlPointName,
                        operation = operation,
                        value = threshold,
                        actualValue = result,
                        pass = checkResult,
                        detail = elementDetail,
                        logPrompt = logPrompt
                    )
                )
            }
        }
        return Pair(allCheckResult, interceptList)
    }

    /**
     * 获取单个拦截成功信息
     */
    private fun getRuleCheckSingleResult(
        ruleName: String,
        interceptRecordList: List<QualityRuleInterceptRecord>,
        params: Map<String, String>
    ): RuleCheckSingleResult {
        val messageList = interceptRecordList.map {
            val thresholdOperationName = ThresholdOperationUtil.getOperationName(it.operation)

            val sb = StringBuilder()
            if (it.pass) {
                sb.append(I18nUtil.getCodeLanMessage(BK_PASSED))
            } else {
                sb.append(I18nUtil.getCodeLanMessage(BK_BLOCKED))
            }
            val nullMsg = if (it.actualValue == null) I18nUtil.getCodeLanMessage(BK_NO_TOOL_OR_RULE_ENABLED) else ""
            val detailMsg = getDetailMsg(it, params)
            Triple(
                sb.append(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_CURRENT_VALUE,
                        params = arrayOf(
                            it.indicatorName,
                            "${it.actualValue}",
                            "$thresholdOperationName${it.value}。 $nullMsg"
                        )
                    )
                ).toString(),
                detailMsg,
                it.pass
            )
        }
        return RuleCheckSingleResult(ruleName, messageList)
    }

    private fun getDetailMsg(record: QualityRuleInterceptRecord, params: Map<String, String>): String {
        // codecc跳到独立入口页面
        return if (CodeccUtils.isCodeccAtom(record.indicatorType) ||
            CodeccUtils.isCodeccCommunityAtom(record.indicatorType)) {
            val projectId = params["projectId"] ?: ""
            val pipelineId = params["pipelineId"] ?: ""
            val buildId = params["buildId"] ?: ""
            val taskId = params[CodeccUtils.BK_CI_CODECC_TASK_ID] ?: ""
            if (taskId.isBlank()) {
                logger.warn("taskId is null or blank for project($projectId) pipeline($pipelineId)")
                return ""
            }
            val bkSeeDetails = I18nUtil.getCodeLanMessage(BK_VIEW_DETAILS)
            if (record.detail.isNullOrBlank()) { // #4796 日志展示的链接去掉域名
                "<a target='_blank' href='/console/codecc/$projectId/task/$taskId/detail'>$bkSeeDetails</a>"
            } else {
                val detailUrl = if (!record.logPrompt.isNullOrBlank()) {
                    record.logPrompt!!
                } else {
                    codeccToolUrlPathMap[record.detail!!] ?: DEFAULT_CODECC_URL
                }
                val fillDetailUrl = detailUrl.replace("##projectId##", projectId)
                    .replace("##taskId##", taskId)
                    .replace("##buildId##", buildId)
                    .replace("##detail##", record.detail!!)
                "<a target='_blank' href='/console$fillDetailUrl'>$bkSeeDetails</a>"
            }
        } else {
            record.logPrompt ?: ""
        }
    }

    /**
     * 记录拦截历史
     */
    private fun recordHistory(
        buildCheckParams: BuildCheckParams,
        result: List<Triple<QualityRule, Boolean, List<QualityRuleInterceptRecord>>>
    ): Int {
        val time = LocalDateTime.now()

        return with(buildCheckParams) {
            result.map {
                val rule = it.first
                val ruleId = HashUtil.decodeIdToLong(rule.hashId)
                val pass = it.second
                val interceptRecordList = it.third

                val interceptList = objectMapper.writeValueAsString(interceptRecordList)
                if (pass) {
                    historyService.serviceCreate(
                        projectId = projectId,
                        ruleId = ruleId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        result = RuleInterceptResult.PASS.name,
                        interceptList = interceptList,
                        createTime = time,
                        updateTime = time
                    )
                } else {
                    val result = if (rule.operation == RuleOperation.AUDIT && null != rule.auditUserList) {
                        RuleInterceptResult.WAIT.name
                    } else {
                        RuleInterceptResult.FAIL.name
                    }
                    historyService.serviceCreate(
                        projectId = projectId,
                        ruleId = ruleId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        result = result,
                        interceptList = interceptList,
                        createTime = time,
                        updateTime = time
                    )
                }
            }.firstOrNull() ?: 1
        }
    }

    private fun sendAuditNotification(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: String,
        createTime: LocalDateTime,
        resultList: List<RuleCheckSingleResult>,
        auditNotifyUserList: List<String>,
        position: String,
        stageId: String?,
        runtimeVariable: Map<String, String>?
    ) {
        val projectName = getProjectName(projectId)
        val pipelineName = runtimeVariable?.get(PIPELINE_NAME) ?: ""
        val url = qualityUrlBean.genBuildDetailUrl(projectId, pipelineId, buildId, position, stageId, runtimeVariable)
        val time = createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))

        // 获取通知用户集合
        val notifyUserSet = auditNotifyUserList.toMutableSet()
        val triggerUserId = runtimeVariable?.get(PIPELINE_START_USER_NAME)
            ?: runtimeVariable?.get(PIPELINE_START_USER_ID) ?: ""
        notifyUserSet.add(triggerUserId)

        val messageResult = StringBuilder()
        val emailResult = StringBuilder()
        val bkInterceptionRulesI18n = I18nUtil.getCodeLanMessage(BK_INTERCEPTION_RULES)
        val bkInterceptionMetricsI18n = I18nUtil.getCodeLanMessage(BK_INTERCEPTION_METRICS)
        resultList.forEach { r ->
            messageResult.append("$bkInterceptionRulesI18n：${r.ruleName}\n")
            messageResult.append("$bkInterceptionMetricsI18n：\n")
            emailResult.append("$bkInterceptionRulesI18n：${r.ruleName}<br>")
            emailResult.append("$bkInterceptionMetricsI18n：<br>")
            r.messagePairs.forEach {
                messageResult.append(it.first + "\n")
                emailResult.append(it.first + "<br>")
            }
            emailResult.append("<br>")
        }

        // 推送消息
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = PIPELINE_QUALITY_AUDIT_NOTIFY_TEMPLATE_V2,
            receivers = notifyUserSet,
            cc = mutableSetOf(triggerUserId),
            titleParams = mapOf(
                "projectName" to projectName,
                "pipelineName" to pipelineName,
                "buildNo" to buildNo
            ),
            bodyParams = mapOf(
                "title" to I18nUtil.getCodeLanMessage(
                    messageCode = BK_BUILD_INTERCEPTED_TO_BE_REVIEWED,
                    params = arrayOf(pipelineName, buildNo, "$auditNotifyUserList")
                ),
                "projectName" to projectName,
                "cc" to triggerUserId,
                "time" to time,
                "result" to messageResult.toString(),
                "emailResult" to emailResult.toString(),
                "url" to url
            )
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("[$buildNo]|sendAuditNotification|QualityRuleCheckService|result=$sendNotifyResult")
    }

    /**
     * 发送终止或者审核通知
     */
    private fun sendEndNotification(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildNo: String,
        createTime: LocalDateTime,
        resultList: List<RuleCheckSingleResult>,
        endNotifyTypeList: List<NotifyType>,
        endNotifyGroupList: List<String>,
        endNotifyUserList: List<String>,
        position: String,
        stageId: String?,
        runtimeVariable: Map<String, String>?
    ) {
        val projectName = getProjectName(projectId)
        val pipelineName = runtimeVariable?.get(PIPELINE_NAME) ?: ""
        val url = qualityUrlBean.genBuildDetailUrl(projectId, pipelineId, buildId, position, stageId, runtimeVariable)
        val time = createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // 获取通知用户集合
        val notifyUserSet = mutableSetOf<String>()

        val groupUsers = qualityNotifyGroupService.serviceGetUsers(endNotifyGroupList)
        // 获取构建触发人
        val triggerUserId = runtimeVariable?.get(PIPELINE_START_USER_NAME)
            ?: runtimeVariable?.get(PIPELINE_START_USER_ID) ?: ""
        notifyUserSet.addAll(groupUsers.innerUsers)
        notifyUserSet.addAll(endNotifyUserList)

        if (triggerUserId.isNotBlank()) {
            notifyUserSet.add(triggerUserId)
        }

        val messageResult = StringBuilder()
        val emailResult = StringBuilder()
        val bkInterceptionRulesI18n = I18nUtil.getCodeLanMessage(BK_INTERCEPTION_RULES)
        val bkInterceptionMetricsI18n = I18nUtil.getCodeLanMessage(BK_INTERCEPTION_METRICS)
        resultList.forEach { r ->
            messageResult.append("$bkInterceptionRulesI18n：${r.ruleName}\n")
            messageResult.append("$bkInterceptionMetricsI18n：\n")
            emailResult.append("$bkInterceptionRulesI18n：${r.ruleName}<br>")
            emailResult.append("$bkInterceptionMetricsI18n：<br>")
            r.messagePairs.forEach {
                messageResult.append(it.first + "\n")
                emailResult.append(it.first + "<br>")
            }
            emailResult.append("<br>")
        }

        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = PIPELINE_QUALITY_END_NOTIFY_TEMPLATE_V2,
            receivers = notifyUserSet,
            cc = mutableSetOf(triggerUserId),
            notifyType = endNotifyTypeList.map { it.name }.toMutableSet(),
            titleParams = mapOf(),
            bodyParams = mapOf(
                "title" to I18nUtil.getCodeLanMessage(
                    messageCode = BK_BUILD_INTERCEPTED_TERMINATED,
                    params = arrayOf(pipelineName, buildNo)
                ),
                "projectName" to projectName,
                "cc" to triggerUserId,
                "time" to time,
                // "thresholdListString" to interceptList.joinToString("；"),
                "result" to messageResult.toString(),
                "emailResult" to emailResult.toString(),
                "url" to url
            )
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("[$buildId]|sendAuditNotification|result=$sendNotifyResult")
    }

    fun getAuditUserList(projectId: String, pipelineId: String, buildId: String, taskId: String): Set<String> {
        val interceptList = historyService.serviceListByBuildIdAndResult(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            result = RuleInterceptResult.WAIT.name
        )
        val ruleIdList = interceptList.map { it.ruleId }

        val auditUserList = mutableSetOf<String>()
        val ruleRecordList = ruleService.serviceListRuleByIds(projectId, ruleIdList.toSet())
        ruleRecordList.forEach {
            val auditNotifyUserList = it.auditUserList ?: listOf()
            if (it.controlPoint.name == taskId) {
                auditUserList.addAll(auditNotifyUserList)
            }
        }

        return auditUserList
    }

    private fun getProjectName(projectId: String): String {
        val project = client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectId)).data?.firstOrNull()
        return project?.projectName ?: throw OperationException("ProjectId: $projectId not exist")
    }

    private fun handleWithMultiIndicator(
        indicators: List<QualityIndicator>,
        metadataList: List<QualityHisMetadata>
    ): Pair<List<QualityIndicator>, List<QualityHisMetadata>> {
        val indicatorsCopy = indicators.toMutableList()
        val metadataListCopy = metadataList.map { it.clone() }

        // // CodeCC插件一个指标的元数据对应多条，先对多个CodeCC插件提前做标识
        val codeccMetaList = metadataListCopy.filter {
            ElementUtils.QUALITY_CODECC_METATYPE.contains(it.elementType)
        }.groupBy { it.taskId }
        if (codeccMetaList.size > 1) {
            codeccMetaList.values.forEachIndexed { index, codeccMeta ->
                codeccMeta.map { it.taskName = "${it.taskName}+$index" }
            }
        }

        indicators.forEach { indicator ->
            // 没有设置taskName时，当输出多个相同指标值，每一个都要加入判断，否则把使用通配符的替换为taskName全名，用于后面加入到指标前缀
            if (indicator.taskName.isNullOrEmpty()) {
                // 没有设置taskName且多个CodeCC插件时，每个要检查的指标额外添加为有对应元数据输出的插件个数
                if (CodeccUtils.isCodeccAtom(indicator.elementType)) {
                    if (codeccMetaList.size > 1) {
                        indicatorsCopy.remove(indicator)
                        codeccMetaList.values.forEach { codeccMeta ->
                            if (codeccMeta.map { it.enName }.containsAll(indicator.metadataList.map { it.enName })) {
                                handleCodeCCPlugin(indicator, codeccMeta, indicatorsCopy)
                            }
                        }
                    }
                } else {
                    // 脚本及三方插件可直接用指标英文名判断，并对结果元数据和指标taskName添加标识后缀
                    if (metadataListCopy.count { it.enName == indicator.enName } > 1) {
                        indicatorsCopy.remove(indicator)
                        metadataListCopy.filter {
                            it.enName == indicator.enName
                        }.forEachIndexed { index, metadata ->
                            handleScriptAndThirdPlugin(indicator, metadata, index, indicatorsCopy)
                        }
                    }
                }
            } else {
                // 设置了taskName的CodeCC插件指标，将匹配到的元数据taskName赋给检查指标的taskName，统一后面对taskName不为空的去标识后缀
                if (CodeccUtils.isCodeccAtom(indicator.elementType)) {
                    codeccMetaList.values.forEach { codeccMeta ->
                        if (codeccMeta.firstOrNull()?.taskName?.startsWith(indicator.taskName ?: "") == true) {
                            indicatorsCopy.remove(indicator)
                            handleCodeCCPlugin(indicator, codeccMeta, indicatorsCopy)
                        }
                    }
                } else {
                    // 脚本及三方插件设置了taskName，将匹配到的结果元数据和指标taskName均加上标识后缀
                    metadataListCopy.filter { it.enName == indicator.enName &&
                            it.taskName.startsWith(indicator.taskName ?: "")
                    }.forEachIndexed { index, metadata ->
                        indicatorsCopy.remove(indicator)
                        handleScriptAndThirdPlugin(indicator, metadata, index, indicatorsCopy)
                    }
                }
            }
        }
        return Pair(indicatorsCopy, metadataListCopy)
    }

    private fun handleScriptAndThirdPlugin(
        indicator: QualityIndicator,
        metadata: QualityHisMetadata,
        index: Int,
        indicatorsCopy: MutableList<QualityIndicator>
    ) {
        // 非CodeCC插件的指标和对应元数据taskName添加标识后缀
        val extraIndicator = indicator.copy()
        val extraTaskName = "${metadata.taskName}+$index"
        extraIndicator.taskName = extraTaskName
        metadata.taskName = extraTaskName
        indicatorsCopy.add(extraIndicator)
    }

    private fun handleCodeCCPlugin(
        indicator: QualityIndicator,
        metadata: List<QualityHisMetadata>,
        indicatorsCopy: MutableList<QualityIndicator>
    ) {
        // 将指标taskName修改为元数据的taskName
        val extraIndicator = indicator.copy()
        extraIndicator.taskName = metadata.firstOrNull()?.taskName
        indicatorsCopy.add(extraIndicator)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityRuleCheckService::class.java)
        private const val DETAIL_NOT_RUN_VALUE = "-1"
        private const val DEFAULT_TIMEOUT_MINUTES = 15
        val DETAIL_NOT_RUN_FLOAT_VALUE = BigDecimal(-1)
    }
}
