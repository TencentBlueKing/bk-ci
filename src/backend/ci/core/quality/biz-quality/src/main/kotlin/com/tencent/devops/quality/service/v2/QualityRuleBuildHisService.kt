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
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.api.v3.pojo.response.RuleCreateResponseV3
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.exception.QualityOpConfigException
import com.tencent.devops.quality.pojo.enum.RuleOperation
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QualityRuleBuildHisService constructor(
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityIndicatorService: QualityIndicatorService,
    private val indicatorService: QualityIndicatorService,
    private val dslContext: DSLContext
) {

    private val logger = LoggerFactory.getLogger(QualityRuleBuildHisService::class.java)

    fun serviceCreate(
        userId: String,
        projectId: String,
        pipelineId: String,
        ruleRequestList: List<RuleCreateRequestV3>
    ): List<RuleCreateResponseV3> {
        checkRuleRequest(ruleRequestList)

        return ruleRequestList.map { ruleRequest ->
            logger.info("start to create rule: $projectId, $pipelineId, ${ruleRequest.name}")
            val indicatorIds = mutableListOf<RuleCreateRequest.CreateRequestIndicator>()

            ruleRequest.indicators.groupBy { it.atomCode }.forEach { (atomCode, indicators) ->
                val indicatorMap = indicators.map { it.enName to it }.toMap()
                indicatorService.serviceList(atomCode, indicators.map { it.enName }).forEach {
                    indicatorIds.add(RuleCreateRequest.CreateRequestIndicator(
                        it.hashId,
                        indicatorMap[it.enName]!!.operation,
                        indicatorMap[it.enName]!!.threshold
                    ))
                }
            }

            logger.info("start to create rule snapshot: $projectId, $pipelineId, ${ruleRequest.name}")
            val id = qualityRuleBuildHisDao.create(dslContext, userId, projectId, pipelineId, ruleRequest, indicatorIds)

            RuleCreateResponseV3(ruleRequest.name, projectId, pipelineId, HashUtil.encodeLongId(id))
        }
    }

    fun list(ruleBuildIds: Collection<Long>): List<QualityRule> {
        logger.info("start to check rule in his: $ruleBuildIds")
        val allRule = qualityRuleBuildHisDao.list(dslContext, ruleBuildIds)

        logger.info("start to check rule op list in his: ${allRule.size}")

        val allIndicatorIds = mutableSetOf<Long>()
        allRule.forEach {
            allIndicatorIds.addAll(it.indicatorIds.split(",").map { indicatorId -> indicatorId.toLong() })
        }

        logger.info("start to check rule indicator: ${allIndicatorIds.firstOrNull()}, ${allIndicatorIds.size}")
        val qualityIndicatorMap = qualityIndicatorService.serviceList(allIndicatorIds).map {
            HashUtil.decodeIdToLong(it.hashId).toString() to it
        }.toMap()
        return allRule.map {
            val thresholdList = it.indicatorThresholds.split(",")
            val opList = it.indicatorOperations.split(",")
            val ruleIndicatorIdMap = it.indicatorIds.split(",").mapIndexed { index, id ->
                id.toLong() to Pair(opList[index], thresholdList[index])
            }.toMap()

            val rule = QualityRule(
                hashId = HashUtil.encodeLongId(it.id),
                name = it.ruleName,
                desc = it.ruleDesc,
                indicators = it.indicatorIds.split(",").map INDICATOR@{ indicatorId ->
                    val indicator = qualityIndicatorMap[indicatorId]
                        ?: throw IllegalArgumentException("indicatorId not found: $indicatorId, $qualityIndicatorMap")

                    val indicatorCopy = indicator.clone()

                    val item = ruleIndicatorIdMap[indicatorId.toLong()]

                    indicatorCopy.operation = QualityOperation.valueOf(item?.first ?: indicator.operation.name)
                    indicatorCopy.threshold = item?.second ?: indicator.threshold

                    return@INDICATOR indicatorCopy
                },
                controlPoint = QualityRule.RuleControlPoint(
                    "", "", "", ControlPointPosition(ControlPointPosition.AFTER_POSITION), listOf()
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
                }
            )
            rule
        }
    }

    private fun checkRuleRequest(ruleRequestList: List<RuleCreateRequestV3>) {
        ruleRequestList.forEach { request ->
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

                if (request.indicators.isEmpty()) {
                    throw QualityOpConfigException("quality rule indicators is empty")
                }
            }
        }
    }

    fun updateBuildId(ruleBuildIds: Collection<Long>, buildId: String) {
        val count = qualityRuleBuildHisDao.updateBuildId(ruleBuildIds, buildId)
        logger.info("finish to update rule build his build id: $count")
    }
}
