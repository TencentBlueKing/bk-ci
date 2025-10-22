/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.process.tables.TPipelineRule
import com.tencent.devops.model.process.tables.records.TPipelineRuleRecord
import com.tencent.devops.process.pojo.pipeline.PipelineRule
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineRuleDao {

    fun countByName(dslContext: DSLContext, ruleName: String, busCode: String): Int {
        with(TPipelineRule.T_PIPELINE_RULE) {
            return dslContext.selectCount().from(this).where(RULE_NAME.eq(ruleName).and(BUS_CODE.eq(busCode)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun add(dslContext: DSLContext, pipelineRule: PipelineRule, userId: String) {
        with(TPipelineRule.T_PIPELINE_RULE) {
            dslContext.insertInto(
                this,
                ID,
                RULE_NAME,
                BUS_CODE,
                PROCESSOR,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    pipelineRule.ruleName,
                    pipelineRule.busCode,
                    pipelineRule.processor,
                    userId,
                    userId
                )
                .onDuplicateKeyUpdate()
                .set(PROCESSOR, pipelineRule.processor)
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, ruled: String) {
        with(TPipelineRule.T_PIPELINE_RULE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(ruled))
                .execute()
        }
    }

    fun update(dslContext: DSLContext, ruleId: String, pipelineRule: PipelineRule, userId: String) {
        with(TPipelineRule.T_PIPELINE_RULE) {
            dslContext.update(this)
                .set(RULE_NAME, pipelineRule.ruleName)
                .set(BUS_CODE, pipelineRule.busCode)
                .set(PROCESSOR, pipelineRule.processor)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(ruleId))
                .execute()
        }
    }

    fun getPipelineRuleById(dslContext: DSLContext, ruleId: String): TPipelineRuleRecord? {
        with(TPipelineRule.T_PIPELINE_RULE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(ruleId))
                .fetchOne()
        }
    }

    fun getPipelineRuleByName(dslContext: DSLContext, ruleName: String, busCode: String): TPipelineRuleRecord? {
        with(TPipelineRule.T_PIPELINE_RULE) {
            return dslContext.selectFrom(this)
                .where(RULE_NAME.eq(ruleName).and(BUS_CODE.eq(busCode)))
                .fetchOne()
        }
    }

    fun getPipelineRuleCount(
        dslContext: DSLContext,
        ruleName: String? = null,
        busCode: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Long {
        with(TPipelineRule.T_PIPELINE_RULE) {
            val conditions = generateQueryPipelineRuleCondition(ruleName, busCode)
            return dslContext.selectCount().from(this).where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getPipelineRules(
        dslContext: DSLContext,
        ruleName: String? = null,
        busCode: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<TPipelineRuleRecord>? {
        with(TPipelineRule.T_PIPELINE_RULE) {
            val conditions = generateQueryPipelineRuleCondition(ruleName, busCode)
            val baseStep = dslContext.selectFrom(this).where(conditions).orderBy(CREATE_TIME.desc())
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getPipelineRulesByBusCode(
        dslContext: DSLContext,
        busCode: String,
        ruleNameList: List<String>? = null
    ): Result<TPipelineRuleRecord>? {
        with(TPipelineRule.T_PIPELINE_RULE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BUS_CODE.eq(busCode))
            if (ruleNameList != null) {
                conditions.add(RULE_NAME.`in`(ruleNameList))
            }
            return dslContext.selectFrom(this).where(conditions).orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    private fun TPipelineRule.generateQueryPipelineRuleCondition(
        ruleName: String?,
        busCode: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (ruleName != null) {
            conditions.add(RULE_NAME.eq(ruleName))
        }
        if (busCode != null) {
            conditions.add(BUS_CODE.eq(busCode))
        }
        return conditions
    }
}
