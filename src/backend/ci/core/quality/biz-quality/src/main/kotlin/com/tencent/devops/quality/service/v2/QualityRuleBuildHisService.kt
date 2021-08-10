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

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.pojo.enum.RuleOperation
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QualityRuleBuildHisService constructor(
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityIndicatorService: QualityIndicatorService,
    private val qualityRuleOperationService: QualityRuleOperationService,
    private val dslContext: DSLContext
) {

    private val logger = LoggerFactory.getLogger(QualityRuleBuildHisService::class.java)

    fun list(ruleIds: List<Long>): List<QualityRule> {
        logger.info("start to check rule in his: $ruleIds")
        val allRule = qualityRuleBuildHisDao.list(dslContext, ruleIds)

        logger.info("start to check rule op list in his: ${allRule.size}")
        val opList = qualityRuleOperationService.serviceList(dslContext, ruleIds).map {
            QualityRule.RuleOp(
                operation = RuleOperation.valueOf(it.type),
                notifyTypeList = it.notifyTypes?.split(",")?.map {  type ->
                    NotifyType.valueOf(type)
                },
                notifyGroupList = it.notifyGroupId?.split(","),
                notifyUserList = it.notifyUser?.split(","),
                auditUserList = it.auditUser?.split(","),
                auditTimeoutMinutes = it.auditTimeout
            )
        }

        val allIndicatorIds = mutableSetOf<Long>()
        allRule.forEach {
            allIndicatorIds.addAll(it.indicatorIds.split(",").map { indicatorId -> indicatorId.toLong() })
        }

        logger.info("start to check rule indicator: ${allIndicatorIds.firstOrNull()}, ${allIndicatorIds.size}")
        val qualityIndicatorMap = qualityIndicatorService.serviceList(allIndicatorIds).map {
            HashUtil.decodeIdToLong(it.hashId).toString() to it
        }.toMap()
        return allRule.map {
            val rule = QualityRule(
                hashId = HashUtil.encodeLongId(it.ruleId),
                name = it.ruleName,
                desc = it.ruleDesc,
                indicators = it.indicatorIds.split(",").map { indicatorId ->
                    qualityIndicatorMap[indicatorId]
                        ?: throw IllegalArgumentException("indicatorId not found: $indicatorId, $qualityIndicatorMap")
                },
                controlPoint = QualityRule.RuleControlPoint(
                    "", "", "", ControlPointPosition(ControlPointPosition.AFTER_POSITION), listOf()
                ),
                range = listOf(it.pipelineId!!),
                templateRange = listOf(),
                operation = RuleOperation.END,
                notifyTypeList = null,
                notifyUserList = null,
                notifyGroupList = null,
                auditUserList = null,
                auditTimeoutMinutes = null,
                gatewayId = it.gatewayId,
                opList = opList
            )
            rule
        }
    }
}
