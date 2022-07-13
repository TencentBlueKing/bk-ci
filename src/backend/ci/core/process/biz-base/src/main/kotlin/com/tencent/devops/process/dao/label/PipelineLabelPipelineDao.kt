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

package com.tencent.devops.process.dao.label

import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.model.process.tables.TPipelineLabel
import com.tencent.devops.model.process.tables.TPipelineLabelPipeline
import com.tencent.devops.model.process.tables.records.TPipelineLabelPipelineRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 流水线和标签对应关系表
 */
@Repository
class PipelineLabelPipelineDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        labelId: Long,
        userId: String,
        id: Long? = null
    ) {
        logger.info("Create pipeline-label for pipeline $pipelineId with label $labelId by user $userId")
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                LABEL_ID,
                CREATE_TIME,
                CREATE_USER,
                ID
            )
                .values(
                    projectId,
                    pipelineId,
                    labelId,
                    LocalDateTime.now(),
                    userId,
                    id
                ).onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        pipelineLabelRels: List<Pair<Long, Long?>>,
        userId: String
    ) {
        logger.info("Create pipeline-label for pipeline $pipelineId with labels $pipelineLabelRels by user $userId")
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            pipelineLabelRels.map {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    LABEL_ID,
                    CREATE_TIME,
                    CREATE_USER,
                    ID
                )
                    .values(
                        projectId,
                        pipelineId,
                        it.first,
                        LocalDateTime.now(),
                        userId,
                        it.second
                    ).onDuplicateKeyIgnore()
                    .execute()
            }
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        id: Long,
        userId: String
    ) {
        logger.info("Delete pipeline-label $id by user $userId")
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deleteByPipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String
    ): Int {
        logger.info("Delete pipeline-label of pipeline $pipelineId by user $userId")
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun deleteByLabel(
        dslContext: DSLContext,
        projectId: String,
        labelId: Long,
        userId: String
    ): Int {
        logger.info("Delete pipeline-label of label $labelId by user $userId")
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            return dslContext.deleteFrom(this)
                .where(LABEL_ID.eq(labelId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun listPipelines(
        dslContext: DSLContext,
        projectId: String,
        labelId: Set<Long>
    ): Result<TPipelineLabelPipelineRecord> {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(LABEL_ID.`in`(labelId).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    fun listLabels(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Result<TPipelineLabelPipelineRecord> {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    fun listPipelineLabelRels(
        dslContext: DSLContext,
        pipelineIds: Collection<String>,
        projectId: String
    ): Result<TPipelineLabelPipelineRecord>? {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PIPELINE_ID.`in`(pipelineIds))
            conditions.add(PROJECT_ID.eq(projectId))
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun getPipelineLabelRelateInfos(
        dslContext: DSLContext,
        projectIds: List<String>
    ): List<PipelineLabelRelateInfo> {
        with(TPipelineLabelPipeline.T_PIPELINE_LABEL_PIPELINE) {
            val pipelineLabel = TPipelineLabel.T_PIPELINE_LABEL
            return dslContext.select(
                PROJECT_ID,
                PIPELINE_ID,
                LABEL_ID,
                pipelineLabel.NAME,
                CREATE_USER,
                CREATE_TIME
            ).from(this)
                .join(pipelineLabel)
                .on(LABEL_ID.eq(pipelineLabel.ID))
                .where(this.PROJECT_ID.`in`(projectIds))
                .fetchInto(PipelineLabelRelateInfo::class.java)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineLabelPipelineDao::class.java)
    }
}
