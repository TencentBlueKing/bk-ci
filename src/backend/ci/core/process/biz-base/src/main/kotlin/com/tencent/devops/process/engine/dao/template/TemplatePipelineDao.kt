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

package com.tencent.devops.process.engine.dao.template

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.model.process.Tables
import com.tencent.devops.model.process.tables.TPipelineInfo
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TTemplatePipelineRecord
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("Unused", "LongParameterList", "TooManyFunctions")
@Repository
class TemplatePipelineDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        instanceType: String,
        rootTemplateId: String,
        templateVersion: Long,
        versionName: String,
        templateId: String,
        userId: String,
        buildNo: String?,
        param: String?
    ) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                INSTANCE_TYPE,
                ROOT_TEMPLATE_ID,
                VERSION,
                VERSION_NAME,
                TEMPLATE_ID,
                CREATOR,
                UPDATOR,
                CREATED_TIME,
                UPDATED_TIME,
                BUILD_NO,
                PARAM
            )
                .values(
                    projectId,
                    pipelineId,
                    instanceType,
                    rootTemplateId,
                    templateVersion,
                    versionName,
                    templateId,
                    userId,
                    userId,
                    now,
                    now,
                    buildNo ?: "",
                    param ?: ""
                )
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        pipelineId: String,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): TTemplatePipelineRecord? {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(INSTANCE_TYPE.eq(instanceType))
                .fetchOne()
        }
    }

    fun isTemplatePipeline(
        dslContext: DSLContext,
        pipelineId: String,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): Boolean {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return (dslContext.selectCount()
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(INSTANCE_TYPE.eq(instanceType))
                .fetchOne(0, Long::class.java) ?: 0) > 0
        }
    }

    fun listByPipelines(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): Result<TTemplatePipelineRecord> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .and(INSTANCE_TYPE.eq(instanceType))
                .and(DELETED.eq(false)) // #4012 模板实例列表需要隐藏回收站的流水线
                .fetch()
        }
    }

    fun listByPipelinesId(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): Result<out Record> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.select(PIPELINE_ID.`as`("pipelineId"), TEMPLATE_ID.`as`("templateId"))
                .from(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .and(INSTANCE_TYPE.eq(instanceType))
                .and(DELETED.eq(false)) // #4012 模板实例列表需要隐藏回收站的流水线
                .fetch()
        }
    }

    fun listPipeline(
        dslContext: DSLContext,
        instanceType: String,
        templateIds: Collection<String>,
        deleteFlag: Boolean? = null
    ): Result<TTemplatePipelineRecord> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = getQueryTemplatePipelineCondition(templateIds, instanceType, deleteFlag)
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    private fun TTemplatePipeline.getQueryTemplatePipelineCondition(
        templateIds: Collection<String>,
        instanceType: String,
        deleteFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(TEMPLATE_ID.`in`(templateIds))
        conditions.add(INSTANCE_TYPE.eq(instanceType))
        if (deleteFlag != null) {
            conditions.add(DELETED.eq(deleteFlag))
        }
        return conditions
    }

    fun countByTemplates(
        dslContext: DSLContext,
        instanceType: String,
        templateIds: Collection<String>,
        deleteFlag: Boolean? = null
    ): Int {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = getQueryTemplatePipelineCondition(templateIds, instanceType, deleteFlag)
            return dslContext.select(DSL.count(PIPELINE_ID)).from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun listPipelineInPage(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        instanceType: String,
        page: Int? = null,
        pageSize: Int? = null,
        searchKey: String? = null,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): SQLPage<TTemplatePipelineRecord> {
        val nameLikedPipelineIds = if (!searchKey.isNullOrBlank()) {
            with(TPipelineSetting.T_PIPELINE_SETTING) {
                dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(IS_TEMPLATE.eq(false))
                    .and(NAME.like("%$searchKey%"))
                    .fetch()
                    .map { it.pipelineId }
                    .toSet()
            }
        } else null

        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val baseStep = dslContext.selectFrom(this)
                .where(TEMPLATE_ID.eq(templateId))

            if (!searchKey.isNullOrBlank()) {
                baseStep.and(PIPELINE_ID.`in`(nameLikedPipelineIds))
            }
            baseStep.and(INSTANCE_TYPE.eq(instanceType))
                .and(DELETED.eq(false)) // #4012 模板实例列表需要隐藏 回收站的流水线
            when (sortType) {
                TemplateSortTypeEnum.VERSION -> {
                    baseStep.orderBy(if (desc == false) VERSION else VERSION.desc())
                }
                TemplateSortTypeEnum.UPDATE_TIME -> {
                    baseStep.orderBy(if (desc == false) UPDATED_TIME else UPDATED_TIME.desc())
                }
                else -> baseStep.orderBy(if (desc == false) UPDATED_TIME else UPDATED_TIME.desc())
            }
            val allCount = baseStep.count()
            val records = if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }

            return SQLPage(allCount.toLong(), records)
        }
    }

    fun countByVersionFeat(
        dslContext: DSLContext,
        templateId: String,
        instanceType: String,
        version: Long? = null,
        versionName: String? = null
    ): Int {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(TEMPLATE_ID.eq(templateId))
            conditions.add(INSTANCE_TYPE.eq(instanceType))
            if (null != version) {
                conditions.add(VERSION.eq(version))
            }
            if (null != versionName) {
                conditions.add(VERSION_NAME.eq(versionName))
            }
            conditions.add(DELETED.eq(false)) // #4012 模板实例列表需要隐藏 回收站的流水线
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun restore(dslContext: DSLContext, pipelineId: String) {
        return with(Tables.T_TEMPLATE_PIPELINE) {
            dslContext.update(this).set(DELETED, false)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(DELETED.eq(true))
                .execute()
        }
    }

    fun softDelete(dslContext: DSLContext, pipelineId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.update(this)
                .set(DELETED, true)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteByTemplateId(dslContext: DSLContext, templateId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId))
                .execute()
        }
    }

    fun deleteByVersion(dslContext: DSLContext, templateId: String, version: Long) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId).and(VERSION.eq(version)))
                .execute()
        }
    }

    fun deleteByVersionName(dslContext: DSLContext, templateId: String, versionName: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId).and(VERSION_NAME.eq(versionName)))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, pipelineId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        templateVersion: Long,
        versionName: String,
        userId: String,
        instance: TemplateInstanceUpdate
    ): Int {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.update(this)
                .set(VERSION, templateVersion)
                .set(VERSION_NAME, versionName)
                .set(UPDATOR, userId)
                .set(BUILD_NO, instance.buildNo?.let { self -> JsonUtil.toJson(self, formatted = false) })
                .set(PARAM, instance.param?.let { self -> JsonUtil.toJson(self, formatted = false) })
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(instance.pipelineId))
                .execute()
        }
    }

    fun listPipelineTemplate(
        dslContext: DSLContext,
        pipelineIds: Collection<String>
    ): Result<TTemplatePipelineRecord>? {
        return with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
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
        return dslContext.select(DSL.countDistinct(t1.TEMPLATE_ID))
            .from(t1).join(t2).on(t1.PIPELINE_ID.eq(t2.PIPELINE_ID))
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
