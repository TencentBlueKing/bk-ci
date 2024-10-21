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

import com.tencent.devops.model.process.tables.TPipelineSubRef
import com.tencent.devops.model.process.tables.records.TPipelineSubRefRecord
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class SubPipelineRefDao {
    fun batchAdd(
        dslContext: DSLContext,
        subPipelineRefList: Collection<SubPipelineRef>
    ) {
        if (subPipelineRefList.isEmpty()) {
            return
        }

        with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            subPipelineRefList.forEach {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    CHANNEL,
                    STAGE_NAME,
                    CONTAINER_NAME,
                    TASK_ID,
                    TASK_NAME,
                    SUB_PROJECT_ID,
                    SUB_PIPELINE_ID,
                    SUB_PIPELINE_NAME,
                    CONTAINER_SEQ,
                    TASK_PROJECT_ID,
                    TASK_PIPELINE_TYPE,
                    TASK_PIPELINE_ID,
                    TASK_PIPELINE_NAME
                ).values(
                    it.projectId,
                    it.pipelineId,
                    it.pipelineName,
                    it.channel,
                    it.stageName,
                    it.containerName,
                    it.element.id,
                    it.element.name,
                    it.subProjectId,
                    it.subPipelineId,
                    it.subPipelineName,
                    it.containerSeq,
                    it.taskProjectId,
                    it.taskPipelineType.name,
                    it.taskPipelineId,
                    it.taskPipelineName
                ).onDuplicateKeyUpdate()
                    .set(STAGE_NAME, it.stageName)
                    .set(CONTAINER_NAME, it.containerName)
                    .set(TASK_NAME, it.element.name)
                    .set(PIPELINE_NAME, it.pipelineName)
                    .set(SUB_PROJECT_ID, it.subProjectId)
                    .set(SUB_PIPELINE_ID, it.subPipelineId)
                    .set(SUB_PIPELINE_NAME, it.subPipelineName)
                    .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Result<TPipelineSubRefRecord> {
        return with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            dslContext.selectFrom(this).where(
                PROJECT_ID.eq(projectId).and(
                    PIPELINE_ID.eq(pipelineId)
                )
            ).fetch()
        }
    }

    fun deleteAll(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        return with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            dslContext.deleteFrom(this).where(
                PROJECT_ID.eq(projectId).and(
                    PIPELINE_ID.eq(pipelineId)
                )
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): Int {
        return with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            dslContext.deleteFrom(this).where(
                listOf(
                    PROJECT_ID.eq(projectId),
                    PIPELINE_ID.eq(pipelineId),
                    TASK_ID.eq(taskId)
                )
            ).execute()
        }
    }

    fun batchDelete(
        dslContext: DSLContext,
        ids: List<Long>
    ) {
        if (ids.isEmpty()) {
            return
        }
        with(TPipelineSubRef.T_PIPELINE_SUB_REF) {
            dslContext.deleteFrom(this).where(
                ID.`in`(ids)
            ).execute()
        }
    }
}
