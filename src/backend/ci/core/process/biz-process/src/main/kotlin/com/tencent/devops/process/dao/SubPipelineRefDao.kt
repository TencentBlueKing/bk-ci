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

import com.tencent.devops.model.process.tables.TSubPipelineRef
import com.tencent.devops.model.process.tables.records.TSubPipelineRefRecord
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class SubPipelineRefDao {
    fun batchAdd(
        dslContext: DSLContext,
        subPipelineRefList: List<SubPipelineRef>
    ) {
        with(TSubPipelineRef.T_SUB_PIPELINE_REF) {
            dslContext.batch(
                subPipelineRefList.map {
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
                        SUB_PIPELINE_ID
                    ).values(
                        it.projectId,
                        it.pipelineId,
                        it.pipelineName,
                        it.channel,
                        it.stageName,
                        it.containerName,
                        it.taskId,
                        it.taskName,
                        it.subProjectId,
                        it.subPipelineId
                    ).onDuplicateKeyUpdate()
                        .set(STAGE_NAME,it.stageName)
                        .set(CONTAINER_NAME,it.containerName)
                        .set(TASK_NAME, it.taskName)
                        .set(PIPELINE_NAME, it.pipelineName)
                        .set(SUB_PROJECT_ID, it.subProjectId)
                        .set(SUB_PIPELINE_ID, it.subPipelineId)
                }
            ).execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId:String,
        pipelineId:String
    ) :Result<TSubPipelineRefRecord>{
        return with(TSubPipelineRef.T_SUB_PIPELINE_REF){
            dslContext.selectFrom(this).where(
                PROJECT_ID.eq(projectId).and(
                    PIPELINE_ID.eq(pipelineId)
                )
            ).fetch()
        }
    }
}
