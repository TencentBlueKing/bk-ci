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

import com.tencent.devops.common.api.constant.KEY_UPDATED_TIME
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.KEY_VERSION_NAME
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.model.process.Tables
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.TTemplatePipeline
import com.tencent.devops.model.process.tables.records.TTemplatePipelineRecord
import com.tencent.devops.process.pojo.enums.TemplateSortTypeEnum
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_TEMPLATE_ID
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.Record4
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
        projectId: String,
        pipelineId: String,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): TTemplatePipelineRecord? {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(INSTANCE_TYPE.eq(instanceType))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
    }

    fun isTemplatePipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): Boolean {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return (dslContext.selectCount()
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(INSTANCE_TYPE.eq(instanceType))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java) ?: 0) > 0
        }
    }

    fun listByPipelines(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type,
        projectId: String? = null
    ): Result<Record1<String>> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            conditions.add(INSTANCE_TYPE.eq(instanceType))
            conditions.add(DELETED.eq(false)) // #4012 模板实例列表需要隐藏回收站的流水线
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            return dslContext.select(PIPELINE_ID).from(this)
                .where(conditions)
                .fetch()
        }
    }

    /**
     * 获取简要信息(避免大字段)
     *
     * @return PIPELINE_ID, TEMPLATE_ID
     */
    fun listSimpleByPipelines(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type,
        projectId: String? = null
    ): Result<Record4<String, String, Long, String>> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            conditions.add(DELETED.eq(false)) // #4012 模板实例列表需要隐藏回收站的流水线
            conditions.add(INSTANCE_TYPE.eq(instanceType))
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            return dslContext.select(PIPELINE_ID, TEMPLATE_ID, VERSION, VERSION_NAME).from(this)
                .where(conditions)
                .fetch()
        }
    }

    fun listByPipelinesId(
        dslContext: DSLContext,
        pipelineIds: Set<String>,
        instanceType: String? = PipelineInstanceTypeEnum.CONSTRAINT.type
    ): Result<out Record> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            return dslContext.select(PIPELINE_ID.`as`(KEY_PIPELINE_ID), TEMPLATE_ID.`as`(KEY_TEMPLATE_ID))
                .from(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .and(INSTANCE_TYPE.eq(instanceType))
                .and(DELETED.eq(false)) // #4012 模板实例列表需要隐藏回收站的流水线
                .fetch()
        }
    }

    fun listPipeline(
        dslContext: DSLContext,
        projectId: String,
        instanceType: String,
        templateIds: Collection<String>,
        deleteFlag: Boolean? = null
    ): Result<Record3<String, String, Long>> {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = getQueryTemplatePipelineCondition(projectId, templateIds, instanceType, deleteFlag)
            return dslContext.select(
                PIPELINE_ID.`as`(KEY_PIPELINE_ID),
                TEMPLATE_ID.`as`(KEY_TEMPLATE_ID),
                VERSION.`as`(KEY_VERSION)
            )
                .from(this)
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

    fun countByTemplates(
        dslContext: DSLContext,
        projectId: String,
        instanceType: String,
        templateIds: Collection<String>,
        deleteFlag: Boolean? = null
    ): Int {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = getQueryTemplatePipelineCondition(projectId, templateIds, instanceType, deleteFlag)
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
        page: Int,
        pageSize: Int,
        searchKey: String? = null,
        sortType: TemplateSortTypeEnum?,
        desc: Boolean?
    ): SQLPage<Record> {
        val nameLikedPipelineIds = if (!searchKey.isNullOrBlank()) {
            with(TPipelineSetting.T_PIPELINE_SETTING) {
                dslContext.select(PIPELINE_ID)
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(NAME.like("%$searchKey%"))
                    .groupBy(PIPELINE_ID)
                    .fetch()
                    .map { it.value1() }
            }
        } else null

        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val baseStep = dslContext.select(
                PIPELINE_ID.`as`(KEY_PIPELINE_ID),
                TEMPLATE_ID.`as`(KEY_TEMPLATE_ID),
                VERSION.`as`(KEY_VERSION),
                VERSION_NAME.`as`(KEY_VERSION_NAME),
                UPDATED_TIME.`as`(KEY_UPDATED_TIME)
            )
                .from(this)
                .where(TEMPLATE_ID.eq(templateId))

            if (!searchKey.isNullOrBlank()) {
                baseStep.and(PIPELINE_ID.`in`(nameLikedPipelineIds))
            }
            baseStep.and(INSTANCE_TYPE.eq(instanceType))
                .and(DELETED.eq(false)) // #4012 模板实例列表需要隐藏 回收站的流水线
                .and(PROJECT_ID.eq(projectId))
            when (sortType) {
                TemplateSortTypeEnum.VERSION -> {
                    baseStep.orderBy(if (desc == false) VERSION else VERSION.desc(), PIPELINE_ID)
                }
                TemplateSortTypeEnum.UPDATE_TIME -> {
                    baseStep.orderBy(if (desc == false) UPDATED_TIME else UPDATED_TIME.desc(), PIPELINE_ID)
                }
                else -> baseStep.orderBy(if (desc == false) UPDATED_TIME else UPDATED_TIME.desc(), PIPELINE_ID)
            }
            val allCount = baseStep.count()
            val records = baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            return SQLPage(allCount.toLong(), records)
        }
    }

    fun countByVersionFeat(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        instanceType: String,
        version: Long? = null,
        versionName: String? = null
    ): Int {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
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

    fun restore(dslContext: DSLContext, projectId: String, pipelineId: String) {
        return with(Tables.T_TEMPLATE_PIPELINE) {
            dslContext.update(this).set(DELETED, false)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(DELETED.eq(true))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun softDelete(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.update(this)
                .set(DELETED, true)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteByTemplateId(dslContext: DSLContext, projectId: String, templateId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deleteByVersion(dslContext: DSLContext, projectId: String, templateId: String, version: Long) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId).and(VERSION.eq(version)).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deleteByVersionName(dslContext: DSLContext, projectId: String, templateId: String, versionName: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_ID.eq(templateId).and(VERSION_NAME.eq(versionName)).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(TTemplatePipeline.T_TEMPLATE_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
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
                .where(PIPELINE_ID.eq(instance.pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }
}
