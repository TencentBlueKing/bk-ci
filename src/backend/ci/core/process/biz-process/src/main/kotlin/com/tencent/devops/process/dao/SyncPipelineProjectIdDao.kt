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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineLabel
import com.tencent.devops.model.process.tables.TPipelineLabelPipeline
import com.tencent.devops.model.process.tables.TPipelinePauseValue
import com.tencent.devops.model.process.tables.TPipelineWebhookQueue
import com.tencent.devops.model.process.tables.TTemplateInstanceItem
import com.tencent.devops.model.process.tables.records.TPipelineLabelPipelineRecord
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.model.process.tables.records.TPipelinePauseValueRecord
import com.tencent.devops.model.process.tables.records.TPipelineWebhookQueueRecord
import com.tencent.devops.model.process.tables.records.TTemplateInstanceItemRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class SyncPipelineProjectIdDao {

    fun listPipelineLabel(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        timeDescFlag: Boolean = true
    ): Result<TPipelineLabelRecord>? {
        return with(TPipelineLabel.T_PIPELINE_LABEL) {
            val conditions = mutableListOf<Condition>()
            val baseQuery = dslContext.selectFrom(this).where(conditions)
            if (timeDescFlag) {
                baseQuery.orderBy(CREATE_TIME.desc(), ID)
            } else {
                baseQuery.orderBy(CREATE_TIME.asc(), ID)
            }
            baseQuery.limit(limit).offset(offset).fetch()
        }
    }

    fun updatePipelineLabelProject(
        dslContext: DSLContext,
        groupId: Long,
        projectId: String
    ) {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .where(GROUP_ID.eq(groupId))
                .execute()
        }
    }

    fun listPipelineLabelPipeline(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        timeDescFlag: Boolean = true
    ): Result<TPipelineLabelPipelineRecord>? {
        return with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            val conditions = mutableListOf<Condition>()
            val baseQuery = dslContext.selectFrom(this).where(conditions)
            if (timeDescFlag) {
                baseQuery.orderBy(CREATE_TIME.desc(), ID)
            } else {
                baseQuery.orderBy(CREATE_TIME.asc(), ID)
            }
            baseQuery.limit(limit).offset(offset).fetch()
        }
    }

    fun updatePipelineLabelPipelineProject(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String
    ) {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun listPipelinePauseValue(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        timeDescFlag: Boolean = true
    ): Result<TPipelinePauseValueRecord>? {
        return with(TPipelinePauseValue.T_PIPELINE_PAUSE_VALUE) {
            val conditions = mutableListOf<Condition>()
            val baseQuery = dslContext.selectFrom(this).where(conditions)
            if (timeDescFlag) {
                baseQuery.orderBy(CREATE_TIME.desc(), BUILD_ID)
            } else {
                baseQuery.orderBy(CREATE_TIME.asc(), BUILD_ID)
            }
            baseQuery.limit(limit).offset(offset).fetch()
        }
    }

    fun updatePipelinePauseValueProject(
        dslContext: DSLContext,
        buildId: String,
        projectId: String
    ) {
        with(TPipelinePauseValue.T_PIPELINE_PAUSE_VALUE) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .where(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun listPipelineWebhookQueue(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        timeDescFlag: Boolean = true
    ): Result<TPipelineWebhookQueueRecord>? {
        return with(TPipelineWebhookQueue.T_PIPELINE_WEBHOOK_QUEUE) {
            val conditions = mutableListOf<Condition>()
            val baseQuery = dslContext.selectFrom(this).where(conditions)
            if (timeDescFlag) {
                baseQuery.orderBy(CREATE_TIME.desc(), PIPELINE_ID)
            } else {
                baseQuery.orderBy(CREATE_TIME.asc(), PIPELINE_ID)
            }
            baseQuery.limit(limit).offset(offset).fetch()
        }
    }

    fun updatePipelineWebhookQueueProject(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String
    ) {
        with(TPipelineWebhookQueue.T_PIPELINE_WEBHOOK_QUEUE) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun listTemplateInstanceItem(
        dslContext: DSLContext,
        limit: Int,
        offset: Int,
        timeDescFlag: Boolean = true
    ): Result<TTemplateInstanceItemRecord>? {
        return with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            val conditions = mutableListOf<Condition>()
            val baseQuery = dslContext.selectFrom(this).where(conditions)
            if (timeDescFlag) {
                baseQuery.orderBy(CREATE_TIME.desc(), ID)
            } else {
                baseQuery.orderBy(CREATE_TIME.asc(), ID)
            }
            baseQuery.limit(limit).offset(offset).fetch()
        }
    }

    fun updateTemplateInstanceItemProject(
        dslContext: DSLContext,
        baseId: String,
        projectId: String
    ) {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .where(BASE_ID.eq(baseId))
                .execute()
        }
    }
}
