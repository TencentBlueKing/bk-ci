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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.model.quality.tables.TQualityRule
import com.tencent.devops.model.quality.tables.TQualityRuleMap
import com.tencent.devops.model.quality.tables.records.TQualityRuleRecord
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v2.pojo.request.RuleUpdateRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class QualityRuleDao {
    fun create(dslContext: DSLContext, userId: String, projectId: String, ruleRequest: RuleCreateRequest): Long {
        val rule = with(TQualityRule.T_QUALITY_RULE) {
            dslContext.insertInto(
                this,
                NAME,
                DESC,
                INDICATOR_RANGE,
                PIPELINE_TEMPLATE_RANGE,
                CONTROL_POINT,
                CONTROL_POINT_POSITION,
                PROJECT_ID,
                CREATE_USER,
                CREATE_TIME,
                GATEWAY_ID
            )
                .values(
                    ruleRequest.name,
                    ruleRequest.desc,
                    ruleRequest.range.joinToString(","),
                    ruleRequest.templateRange.joinToString(","),
                    ruleRequest.controlPoint,
                    ruleRequest.controlPointPosition,
                    projectId,
                    userId,
                    LocalDateTime.now(),
                    ruleRequest.gatewayId
                )
                .returning(ID)
                .fetchOne()
        }
        with(TQualityRuleMap.T_QUALITY_RULE_MAP) {
            dslContext.insertInto(
                this,
                RULE_ID,
                INDICATOR_IDS,
                INDICATOR_OPERATIONS,
                INDICATOR_THRESHOLDS
            )
                .values(
                    rule.id,
                    ruleRequest.indicatorIds.joinToString(",") { HashUtil.decodeIdToLong(it.hashId).toString() },
                    ruleRequest.indicatorIds.joinToString(",") { it.operation },
                    ruleRequest.indicatorIds.joinToString(",") { it.threshold }

                )
                .execute()
        }
        return rule.id
    }

    fun update(context: DSLContext, userId: String, projectId: String, ruleId: Long, ruleRequest: RuleUpdateRequest) {
        with(TQualityRule.T_QUALITY_RULE) {
            context.update(this)
                .set(NAME, ruleRequest.name)
                .set(DESC, ruleRequest.desc)
                .set(INDICATOR_RANGE, ruleRequest.range.joinToString(","))
                .set(PIPELINE_TEMPLATE_RANGE, ruleRequest.templateRange.joinToString(","))
                .set(CONTROL_POINT, ruleRequest.controlPoint)
                .set(CONTROL_POINT_POSITION, ruleRequest.controlPointPosition)
                .set(UPDATE_USER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(GATEWAY_ID, ruleRequest.gatewayId)
                .where(ID.eq(ruleId))
                .execute()
        }
        with(TQualityRuleMap.T_QUALITY_RULE_MAP) {
            context.update(this)
                .set(
                    INDICATOR_IDS,
                    ruleRequest.indicatorIds.joinToString(",") { HashUtil.decodeIdToLong(it.hashId).toString() })
                .set(INDICATOR_OPERATIONS, ruleRequest.indicatorIds.joinToString(",") { it.operation })
                .set(INDICATOR_THRESHOLDS, ruleRequest.indicatorIds.joinToString(",") { it.threshold })
                .where(RULE_ID.eq(ruleId))
                .execute()
        }
    }

    fun updateEnable(dslContext: DSLContext, ruleId: Long, enable: Boolean) {
        with(TQualityRule.T_QUALITY_RULE) {
            dslContext.update(this)
                .set(ENABLE, enable)
                .where(ID.eq(ruleId))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, ruleId: Long) {
        with(TQualityRule.T_QUALITY_RULE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(ruleId))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, ruleId: Long): TQualityRuleRecord {
        with(TQualityRule.T_QUALITY_RULE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(ruleId))
                .fetchOne() ?: throw NotFoundException("RuleId: $ruleId not found")
        }
    }

    fun getById(dslContext: DSLContext, ruleId: Long): TQualityRuleRecord? {
        with(TQualityRule.T_QUALITY_RULE) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(ruleId).and(ENABLE.eq(true)))
                    .fetchOne() ?: throw NotFoundException("RuleId: $ruleId not found")
        }
    }

    fun count(dslContext: DSLContext, projectId: String): Long {
        with(TQualityRule.T_QUALITY_RULE) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java)
        }
    }

    fun list(dslContext: DSLContext, projectId: String, offset: Int, limit: Int): Result<TQualityRuleRecord>? {
        with(TQualityRule.T_QUALITY_RULE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun list(dslContext: DSLContext, projectId: String): Result<TQualityRuleRecord>? {
        with(TQualityRule.T_QUALITY_RULE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        startTime: LocalDateTime?,
        enable: Boolean = true,
        isDeleted: Boolean = false
    ): Result<TQualityRuleRecord>? {
        with(TQualityRule.T_QUALITY_RULE) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                ENABLE.eq(enable)
            )
            if (startTime != null) conditions.add(CREATE_TIME.le(startTime))

            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, ruleIds: Collection<Long>): Result<TQualityRuleRecord>? {
        with(TQualityRule.T_QUALITY_RULE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ID.`in`(ruleIds))
                .fetch()
        }
    }

    fun plusInterceptTimes(dslContext: DSLContext, ruleId: Long) {
        with(TQualityRule.T_QUALITY_RULE) {
            dslContext.update(this)
                .set(INTERCEPT_TIMES, INTERCEPT_TIMES + 1)
                .where(ID.eq(ruleId))
                .execute()
        }
    }

    fun plusExecuteCount(dslContext: DSLContext, ruleId: Long) {
        with(TQualityRule.T_QUALITY_RULE) {
            dslContext.update(this)
                .set(EXECUTE_COUNT, EXECUTE_COUNT + 1)
                .where(ID.eq(ruleId))
                .execute()
        }
    }

    fun listByPosition(dslContext: DSLContext, projectId: String, position: String): Result<TQualityRuleRecord>? {
        with(TQualityRule.T_QUALITY_RULE) {
            val sql = dslContext.selectFrom(this)
                .where(
                    PROJECT_ID.eq(projectId).and(CONTROL_POINT_POSITION.eq(position.toUpperCase())).and(
                        ENABLE.eq(
                            true
                        )
                    )
                )
            return sql.fetch()
        }
    }
}