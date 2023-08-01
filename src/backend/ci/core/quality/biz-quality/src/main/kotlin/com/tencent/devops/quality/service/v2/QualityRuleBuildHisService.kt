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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.quality.tables.records.TQualityRuleBuildHisRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.enums.IndicatorType
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.api.v2.pojo.request.IndicatorCreate
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.api.v3.pojo.response.RuleCreateResponseV3
import com.tencent.devops.quality.constant.QualityMessageCode.CHANGE_QUALITY_GATE_VALUE
import com.tencent.devops.quality.constant.QualityMessageCode.USER_NEED_PIPELINE_X_PERMISSION
import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.dao.v2.QualityIndicatorDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisOperationDao
import com.tencent.devops.quality.exception.QualityOpConfigException
import com.tencent.devops.quality.pojo.enum.RuleOperation
import com.tencent.devops.quality.pojo.enum.RunElementType
import javax.ws.rs.core.Response
import org.apache.commons.lang3.math.NumberUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Suppress("NestedBlockDepth")
class QualityRuleBuildHisService constructor(
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityIndicatorService: QualityIndicatorService,
    private val indicatorService: QualityIndicatorService,
    private val historyDao: HistoryDao,
    private val qualityRuleBuildHisOperationDao: QualityRuleBuildHisOperationDao,
    private val qualityIndicatorDao: QualityIndicatorDao,
    private val dslContext: DSLContext,
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(QualityRuleBuildHisService::class.java)

    fun serviceCreate(
        userId: String,
        projectId: String,
        pipelineId: String,
        ruleRequestList: List<RuleCreateRequestV3>
    ): List<RuleCreateResponseV3> {
        logger.info("QUALITY|ruleRequestList is: $ruleRequestList")
        checkRuleRequest(ruleRequestList)

        return ruleRequestList.map { ruleRequest ->
            // run插件先创建指标
            // todo performance upsert indicator
            val indicatorCreateList = ruleRequest.indicators.filter { it.atomCode == RunElementType.RUN.elementType }
                .map {
                    IndicatorCreate(
                        name = it.enName,
                        cnName = it.enName,
                        desc = "",
                        dataType = QualityDataType.INT,
                        operation = enumValues<QualityOperation>().toList(),
                        threshold = it.threshold,
                        elementType = it.atomCode
                    )
            }
            qualityIndicatorService.upsertIndicators(
                userId = userId,
                projectId = projectId,
                indicatorCreateList = indicatorCreateList
            )

            logger.info("start to create rule: $projectId, $pipelineId, ${ruleRequest.name}")
            val indicatorIds = mutableListOf<RuleCreateRequest.CreateRequestIndicator>()

            ruleRequest.indicators.groupBy { it.atomCode }.forEach { (atomCode, indicators) ->
                var indicatorsCopy = indicators.toMutableList()
                indicatorService.serviceList(atomCode, indicators.map { it.enName }, projectId)
                    .filterNot { it.elementType == RunElementType.RUN.elementType && it.range != projectId }
                    .filter { it.enable ?: false }.forEach {
                    val requestIndicator = indicatorsCopy.find { indicator -> indicator.enName == it.enName }
                        ?: throw OperationException("${ruleRequest.name} indicator ${it.enName} is not exist")
                    logger.info("QUALITY|requestIndicator is: ${requestIndicator.enName}")

                        // 使用上下文变量表示阈值时不检查类型
                    if (!Regex("\\$\\{\\{.*\\}\\}").matches(requestIndicator.threshold) &&
                            requestIndicator.atomCode != RunElementType.RUN.elementType) {
                        checkThresholdType(requestIndicator, it)
                    }

                    indicatorIds.add(RuleCreateRequest.CreateRequestIndicator(
                        it.hashId,
                        requestIndicator.operation,
                        requestIndicator.threshold
                    ))
                    indicatorsCopy.remove(requestIndicator)
                }
            }

            if (indicatorIds.isEmpty()) {
                val indicatorNameSet = ruleRequest.indicators.map { it.enName }.toList()
                throw OperationException("${ruleRequest.name} $indicatorNameSet indicator is not exist")
            }

            logger.info("start to create rule snapshot: $projectId, $pipelineId, ${ruleRequest.name}, $indicatorIds")
            val id = qualityRuleBuildHisDao.create(dslContext, userId, projectId, pipelineId, ruleRequest, indicatorIds)

            RuleCreateResponseV3(ruleRequest.name, projectId, pipelineId, HashUtil.encodeLongId(id))
        }
    }

    @Suppress("ReturnCount")
    private fun checkThresholdType(
        requestIndicator: RuleCreateRequestV3.CreateRequestIndicator,
        indicator: QualityIndicator
    ) {
        when (indicator.thresholdType) {
            QualityDataType.INT -> {
                if (NumberUtils.isDigits(requestIndicator.threshold)) {
                    return
                }
            }
            QualityDataType.FLOAT -> {
                if (NumberUtils.isCreatable(requestIndicator.threshold)) {
                    return
                }
            }
            QualityDataType.BOOLEAN -> {
                if (requestIndicator.threshold == "true" || requestIndicator.threshold == "false") {
                    return
                }
            }
            else -> {
                if (NumberUtils.isDigits(requestIndicator.threshold)) {
                    return
                }
            }
        }
        throw OperationException(
            I18nUtil.getCodeLanMessage(
                messageCode = CHANGE_QUALITY_GATE_VALUE,
                params = arrayOf(requestIndicator.enName, "${indicator.thresholdType}", requestIndicator.threshold)
            )
        )
    }

    fun list(ruleBuildIds: Collection<Long>): List<QualityRule> {
        logger.info("start to check rule in his: $ruleBuildIds")
        val allRule = qualityRuleBuildHisDao.list(dslContext, ruleBuildIds)

        logger.info("start to check rule op list in his: ${allRule.size}")

        return allRule.map {
            val indicatorIdList = it.indicatorIds.split(",").map { id -> id.toLong() }
            val thresholdList = it.indicatorThresholds.split(",")
            val opList = it.indicatorOperations.split(",")
            val qualityIndicatorList = qualityIndicatorService.serviceListALL(indicatorIdList).toMutableList()
            logger.info("QUALITY|get qualityIndicator: ${qualityIndicatorList.size}")

            val rule = QualityRule(
                hashId = HashUtil.encodeLongId(it.id),
                name = it.ruleName,
                desc = it.ruleDesc,
                indicators = qualityIndicatorList.mapIndexed { index, indicator ->
                    indicator.operation = QualityOperation.valueOf(opList[index])
                    indicator.threshold = thresholdList[index]
                    indicator
                },
                controlPoint = QualityRule.RuleControlPoint(
                    "", "", "",
                    ControlPointPosition.create(ControlPointPosition.AFTER_POSITION), listOf()
                ),
                range = if (it.pipelineRange.isNullOrBlank()) {
                    listOf()
                } else {
                    it.pipelineRange.split(",")
                },
                templateRange = if (it.templateRange.isNullOrBlank()) {
                    listOf()
                } else {
                    it.templateRange.split(",")
                },
                operation = RuleOperation.END,
                notifyTypeList = null,
                notifyUserList = null,
                notifyGroupList = null,
                auditUserList = null,
                auditTimeoutMinutes = null,
                gatewayId = it.gatewayId,
                opList = if (it.operationList.isNullOrBlank()) {
                    listOf()
                } else {
                    JsonUtil.to(it.operationList, object : TypeReference<List<QualityRule.RuleOp>>() {})
                },
                status = if (!it.status.isNullOrBlank()) {
                    RuleInterceptResult.valueOf(it.status)
                } else {
                    null
                },
                gateKeepers = if (it.gateKeepers.isNullOrBlank()) {
                    listOf()
                } else {
                    it.gateKeepers.split(",")
                },
                stageId = it.stageId,
                taskSteps = if (it.taskSteps.isNullOrBlank()) {
                    listOf()
                } else {
                    JsonUtil.to(it.taskSteps, object : TypeReference<List<QualityRule.RuleTask>>() {})
                }
            )
            rule
        }
    }

    @Suppress("NestedBlockDepth")
    private fun checkRuleRequest(ruleRequestList: List<RuleCreateRequestV3>) {
        ruleRequestList.forEach { request ->
            if (request.indicators.isEmpty()) {
                throw QualityOpConfigException("quality rule indicators is empty")
            }

            request.opList?.forEach { op ->
                if (op.operation == RuleOperation.END) {
                    if (op.notifyTypeList.isNullOrEmpty()) {
                        throw QualityOpConfigException("notify type is empty for operation notify")
                    }
                    if (op.notifyGroupList.isNullOrEmpty() && op.notifyUserList.isNullOrEmpty()) {
                        throw QualityOpConfigException("notifyGroupList and notifyUserList is empty for operation end")
                    }
                } else {
                    if (op.auditTimeoutMinutes == null) {
                        throw QualityOpConfigException("auditTimeoutMinutes is empty for operation audit")
                    }
                    if (op.auditUserList.isNullOrEmpty()) {
                        throw QualityOpConfigException("auditUserList is empty for operation audit")
                    }
                }
            }
        }
    }

    fun updateBuildId(ruleBuildIds: Collection<Long>, buildId: String) {
        val count = qualityRuleBuildHisDao.updateBuildId(ruleBuildIds, buildId)
        logger.info("finish to update rule build his build id: $count")
    }

    fun updateStatus(ruleBuildId: Long, status: String): Int {
        val count = qualityRuleBuildHisDao.updateStatus(ruleBuildId, status)
        logger.info("finish to update rule his status: $count, $ruleBuildId, $status")
        return count
    }

    fun convertVariables(ruleList: Collection<QualityRule>, buildCheckParamsV3: BuildCheckParamsV3) {
        ruleList.forEach { it ->
            it.indicators.forEach { indicator ->
                val realThreshold = EnvUtils.parseEnv(
                    indicator.threshold,
                    buildCheckParamsV3.runtimeVariable ?: mapOf()
                )

                if (indicator.isScriptElementIndicator()) {
                    val realThresholdType = checkThresholdType(realThreshold)
                    val userId = buildCheckParamsV3.runtimeVariable?.get("BK_CI_START_USER_ID")
                    val indicatorUpdate = IndicatorUpdate(
                        threshold = realThreshold,
                        thresholdType = realThresholdType.name,
                        type = IndicatorType.CUSTOM
                    )
                    if (userId.isNullOrBlank()) {
                        throw QualityOpConfigException("userId is empty for start ci")
                    }
                    val indicatorUpdateCount = qualityIndicatorDao.update(
                        userId = userId,
                        id = HashUtil.decodeIdToLong(indicator.hashId),
                        indicatorUpdate = indicatorUpdate,
                        dslContext = dslContext
                    )
                    indicator.thresholdType = realThresholdType
                    logger.info("QUALITY|update indicator type count is: $indicatorUpdateCount")
                }
                indicator.threshold = realThreshold
            }
            val indicatorCount = qualityRuleBuildHisDao.updateIndicatorThreshold(HashUtil.decodeIdToLong(it.hashId),
                it.indicators.map { indicator -> indicator.threshold }.joinToString(","))
            logger.info("QUALITY|convert_indicatorThreshold|${it.indicators}|COUNT|$indicatorCount")

            val gateKeepers = (it.gateKeepers ?: listOf()).map { user ->
                EnvUtils.parseEnv(user, buildCheckParamsV3.runtimeVariable ?: mapOf())
            }
            val gateKeeperCount = qualityRuleBuildHisDao.updateGateKeepers(HashUtil.decodeIdToLong(it.hashId),
                gateKeepers?.joinToString(","))

            logger.info("QUALITY|convert_gateKeepers|$gateKeepers|COUNT|$gateKeeperCount")
        }
    }

    fun updateStatusService(userId: String, ruleBuildId: Long, pass: Boolean): Boolean {
        var count = 0

        val rules = qualityRuleBuildHisDao.list(dslContext, listOf(ruleBuildId))
        logger.info("update rule for ruleId: $ruleBuildId")
        rules.forEach {
            if (it.gateKeepers != null) {
                if (it.gateKeepers!!.isEmpty() || !(it.gateKeepers!!.contains(userId))) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.FORBIDDEN.statusCode,
                        errorCode = USER_NEED_PIPELINE_X_PERMISSION
                    )
                }
                val ruleResult = if (pass) RuleInterceptResult.INTERCEPT_PASS.name
                            else RuleInterceptResult.INTERCEPT.name
                logger.info("rule $ruleBuildId update status: $ruleResult, $pass")

                if (checkReview(userId, it, pass)) {
                    count = updateStatus(ruleBuildId, ruleResult)
                    qualityRuleBuildHisOperationDao.create(dslContext, userId, ruleBuildId, it.stageId)
                }
            }
        }
        return count > 0
    }

    fun checkReview(userId: String, record: TQualityRuleBuildHisRecord, pass: Boolean): Boolean {
        val stageRules = qualityRuleBuildHisDao.listStageRules(dslContext, record.buildId, record.stageId)
        var passFlag = false
        var stageFinish = false

        if (stageRules.size == 1) {
            stageFinish = true
            passFlag = true
        } else {
            stageRules.filter { it.id != record.id }.map {
                if (it?.status != RuleInterceptResult.WAIT.name) {
                    stageFinish = true
                } else {
                    stageFinish = false
                    return@map
                }
            }
        }

        if (stageFinish) {
            stageRules.filter { it.id != record.id }.map {
                if (it?.status == RuleInterceptResult.INTERCEPT_PASS.name ||
                    it?.status == RuleInterceptResult.PASS.name) {
                    passFlag = true
                } else {
                    passFlag = false
                    return@map
                }
            }
            logger.info("QUALITY|checkReview|${record.buildId}|passFlag is $passFlag. start to send stageRequest.")
            val ruleHistory = historyDao.list(dslContext, record.projectId, record.pipelineId, null, null,
                null, null, null, null)
            return client.get(ServiceBuildResource::class).qualityTriggerStage(
                userId = userId,
                projectId = record.projectId,
                pipelineId = record.pipelineId,
                buildId = record.buildId,
                stageId = record.stageId,
                qualityRequest = StageQualityRequest(
                    position = record.rulePos,
                    pass = passFlag && pass,
                    checkTimes = ruleHistory.first()?.checkTimes ?: 1
                )
            ).data ?: false
        }
        return true
    }

    fun listRuleBuildHis(ruleBuildIds: Collection<Long>): List<QualityRule> {
        val allRule = qualityRuleBuildHisDao.list(dslContext, ruleBuildIds)
        return allRule.map {
            val rule = QualityRule(
                hashId = HashUtil.encodeLongId(it.id),
                name = it.ruleName,
                desc = it.ruleDesc,
                indicators = listOf(),
                controlPoint = QualityRule.RuleControlPoint(
                    "", "", "",
                    ControlPointPosition.create(ControlPointPosition.AFTER_POSITION), listOf()
                ),
                range = if (it.pipelineRange.isNullOrBlank()) {
                    listOf()
                } else {
                    it.pipelineRange.split(",")
                },
                templateRange = if (it.templateRange.isNullOrBlank()) {
                    listOf()
                } else {
                    it.templateRange.split(",")
                },
                operation = RuleOperation.END,
                notifyTypeList = null,
                notifyUserList = null,
                notifyGroupList = null,
                auditUserList = null,
                auditTimeoutMinutes = null,
                gatewayId = it.gatewayId,
                opList = if (it.operationList.isNullOrBlank()) {
                    listOf()
                } else {
                    JsonUtil.to(it.operationList, object : TypeReference<List<QualityRule.RuleOp>>() {})
                },
                status = if (!it.status.isNullOrBlank()) {
                    RuleInterceptResult.valueOf(it.status)
                } else {
                    null
                },
                gateKeepers = if (it.gateKeepers.isNullOrBlank()) {
                    listOf()
                } else {
                    it.gateKeepers.split(",")
                },
                stageId = it.stageId,
                taskSteps = if (it.taskSteps.isNullOrBlank()) {
                    listOf()
                } else {
                    JsonUtil.to(it.taskSteps, object : TypeReference<List<QualityRule.RuleTask>>() {})
                }
            )
            rule
        }
    }

    private fun checkThresholdType(threshold: String): QualityDataType {
        return if (NumberUtils.isDigits(threshold)) {
            QualityDataType.INT
        } else if (threshold == "true" || threshold == "false") {
            QualityDataType.BOOLEAN
        } else {
            QualityDataType.FLOAT
        }
    }
}
