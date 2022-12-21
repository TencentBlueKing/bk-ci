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

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.model.quality.tables.TQualityRuleBuildHis
import com.tencent.devops.model.quality.tables.records.TQualityRuleBuildHisRecord
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class QualityRuleBuildHisDao @Autowired constructor(
    private val innerDslContext: DSLContext
) {
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        ruleRequest: RuleCreateRequestV3,
        indicatorIds: List<RuleCreateRequest.CreateRequestIndicator>
    ): Long {
        var hisId = 0L
        with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                hisId = transactionContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    RULE_POS,
                    RULE_NAME,
                    RULE_DESC,
                    GATEWAY_ID,
                    PIPELINE_RANGE,
                    TEMPLATE_RANGE,
                    INDICATOR_IDS,
                    INDICATOR_OPERATIONS,
                    INDICATOR_THRESHOLDS,
                    OPERATION_LIST,
                    CREATE_TIME,
                    CREATE_USER,
                    GATE_KEEPERS,
                    STAGE_ID,
                    TASK_STEPS
                ).values(
                    projectId,
                    pipelineId,
                    ruleRequest.position,
                    ruleRequest.name,
                    ruleRequest.desc,
                    ruleRequest.gatewayId,
                    ruleRequest.range?.joinToString(",") ?: "",
                    ruleRequest.templateRange?.joinToString(",") ?: "",
                    indicatorIds.map { HashUtil.decodeIdToLong(it.hashId) }.joinToString(","),
                    indicatorIds.joinToString(",") { it.operation },
                    indicatorIds.joinToString(",") { it.threshold },
                    JsonUtil.toJson(ruleRequest.opList ?: listOf<RuleCreateRequestV3.CreateRequestOp>()),
                    LocalDateTime.now(),
                    userId,
                    ruleRequest.gateKeepers?.joinToString(",") ?: "",
                    ruleRequest.stageId,
                    JsonUtil.toJson(ruleRequest.taskSteps ?: listOf<QualityRule.RuleTask>())
                ).returning(ID).fetchOne()!!.id
                val hashId = HashUtil.encodeLongId(hisId)
                transactionContext.update(this)
                    .set(QUALITY_RULE_HIS_HASH_ID, hashId)
                    .where(ID.eq(hisId))
                    .execute()
            }
        }
        return hisId
    }

    fun list(dslContext: DSLContext, ruleIds: Collection<Long>): Result<TQualityRuleBuildHisRecord> {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            dslContext.selectFrom(this)
                .where(ID.`in`(ruleIds))
                .fetch()
        }
    }

    fun listBuildHisRules(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        ruleBuildId: String
    ): Result<TQualityRuleBuildHisRecord> {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(ruleBuildId))
                .fetch()
        }
    }

    fun cleanQualityRule(daysAgo: Long): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            // delete data days ago
            innerDslContext.deleteFrom(this)
                .where(CREATE_TIME.lt(LocalDateTime.now().minusDays(daysAgo)))
                .execute()
        }
    }

    fun updateBuildId(ruleBuildIds: Collection<Long>, buildId: String?): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            innerDslContext.update(this)
                .set(BUILD_ID, buildId)
                .where(ID.`in`(ruleBuildIds))
                .execute()
        }
    }

    fun updateStatus(ruleBuildId: Long, ruleResult: String?): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            innerDslContext.update(this)
                .set(STATUS, ruleResult)
                .where(ID.eq(ruleBuildId))
                .execute()
        }
    }

    fun batchUpdateStatus(ruleBuildIds: Collection<Long>, ruleResult: String?): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            innerDslContext.update(this)
                .set(STATUS, ruleResult)
                .where(ID.`in`(ruleBuildIds))
                .execute()
        }
    }

    fun listStageRules(dslContext: DSLContext, buildId: String, stageId: String): Result<TQualityRuleBuildHisRecord> {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(STAGE_ID.eq(stageId)))
                .fetch()
        }
    }

    fun updateGateKeepers(ruleBuildId: Long, gateKeepers: String): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            innerDslContext.update(this)
                .set(GATE_KEEPERS, gateKeepers)
                .where(ID.eq(ruleBuildId))
                .execute()
        }
    }

    fun updateIndicatorThreshold(ruleBuildId: Long, indicatorThreshold: String): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            innerDslContext.update(this)
                .set(INDICATOR_THRESHOLDS, indicatorThreshold)
                .where(ID.eq(ruleBuildId))
                .execute()
        }
    }

    fun updateTimeoutRuleStatus(ruleBuildIds: Collection<Long>): Int {
        return with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            innerDslContext.update(this)
                .set(STATUS, RuleInterceptResult.INTERCEPT.name)
                .where(ID.`in`(ruleBuildIds))
                .execute()
        }
    }

    fun getAllRuleBuildHis(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<Record1<Long>>? {
        with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            return dslContext.select(ID).from(this)
                .orderBy(CREATE_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateHashId(
        dslContext: DSLContext,
        id: Long,
        hashId: String
    ) {
        with(TQualityRuleBuildHis.T_QUALITY_RULE_BUILD_HIS) {
            dslContext.update(this)
                .set(QUALITY_RULE_HIS_HASH_ID, hashId)
                .where(ID.eq(id))
                .and(QUALITY_RULE_HIS_HASH_ID.isNull)
                .execute()
        }
    }
}
