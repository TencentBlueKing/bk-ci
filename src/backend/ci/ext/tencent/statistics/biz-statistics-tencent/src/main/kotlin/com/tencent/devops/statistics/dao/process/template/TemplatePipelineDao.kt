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

package com.tencent.devops.statistics.dao.process.template

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TTemplatePipelineRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class TemplatePipelineDao @Autowired constructor(private val objectMapper: ObjectMapper) {

    fun listPipeline(
        dslContext: DSLContext,
        projectId: String,
        instanceType: String,
        templateIds: Collection<String>,
        deleteFlag: Boolean? = null
    ): Result<TTemplatePipelineRecord> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = getQueryTemplatePipelineCondition(projectId, templateIds, instanceType, deleteFlag)
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    private fun TTemplatePipeline.getQueryTemplatePipelineCondition(
        projectId: String,
        templateIds: Collection<String>,
        instanceType: String,
        deleteFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(PROJECT_ID.eq(projectId))
        conditions.add(TEMPLATE_ID.`in`(templateIds))
        conditions.add(INSTANCE_TYPE.eq(instanceType))
        if (deleteFlag != null) {
            conditions.add(DELETED.eq(deleteFlag))
        }
        return conditions
    }

    fun countPipelineInstancedByTemplate(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Record1<Int> {
        // 流水线有软删除，需要过滤
        val t1 = TTemplatePipeline.T_TEMPLATE_PIPELINE.`as`("t1")
        val t2 = TPipelineInfo.T_PIPELINE_INFO.`as`("t2")
        return dslContext.selectCount().from(t1).join(t2).on(t1.PIPELINE_ID.eq(t2.PIPELINE_ID))
            .where(t2.DELETE.eq(false))
            .and(t2.PROJECT_ID.`in`(projectIds))
            .fetchOne()!!
    }

    fun countTemplateInstanced(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Record1<Int> {
        // 模板没有软删除，直接查即可
        val t1 = TTemplatePipeline.T_TEMPLATE_PIPELINE.`as`("t1")
        val t2 = TPipelineInfo.T_PIPELINE_INFO.`as`("t2")
        return dslContext.select(
            DSL.countDistinct(t1.TEMPLATE_ID)
        ).from(t1).join(t2).on(t1.PIPELINE_ID.eq(t2.PIPELINE_ID))
            .where(t2.PROJECT_ID.`in`(projectIds))
            .fetchOne()!!
    }

    /**
     * 查询实例化的原始模板总数
     */
    fun countSrcTemplateInstanced(
        dslContext: DSLContext,
        srcTemplateIds: Set<String>
    ): Record1<Int> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.select(DSL.countDistinct(TEMPLATE_ID)).from(this)
                .where(TEMPLATE_ID.`in`(srcTemplateIds)).fetchOne()!!
        }
    }
}
