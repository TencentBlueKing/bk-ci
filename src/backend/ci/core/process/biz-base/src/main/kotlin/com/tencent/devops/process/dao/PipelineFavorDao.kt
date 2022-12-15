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

import com.tencent.devops.model.process.tables.TPipelineFavor
import com.tencent.devops.model.process.tables.records.TPipelineFavorRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 用户收藏流水线
 */
@Repository
class PipelineFavorDao {

    fun save(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        id: Long? = null
    ) {
        logger.info("Create the pipeline favor for pipeline $pipelineId of project $projectId by user $userId")
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                CREATE_TIME,
                CREATE_USER,
                ID
            )
                .values(
                    projectId,
                    pipelineId,
                    LocalDateTime.now(),
                    userId,
                    id
                )
                .onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String
    ) {
        logger.info("Delete the pipeline favor of pipeline $pipelineId by user $userId")
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(CREATE_USER.eq(userId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteAllUserFavorByPipeline(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        return with(TPipelineFavor.T_PIPELINE_FAVOR) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        userId: String,
        projectId: String
    ): Result<TPipelineFavorRecord> {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            return dslContext.selectFrom(this)
                .where(CREATE_USER.eq(userId))
                .and(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun listByUserId(dslContext: DSLContext, userId: String): Result<TPipelineFavorRecord>? {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            return dslContext.selectFrom(this)
                .where(CREATE_USER.eq(userId))
                .fetch()
        }
    }

    fun countByUserId(dslContext: DSLContext, projectId: String, userId: String): Int {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            return dslContext.selectCount().from(this)
                .where(CREATE_USER.eq(userId))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()?.value1() ?: 0
        }
    }

    fun listByPipelineId(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<TPipelineFavorRecord>? {
        with(TPipelineFavor.T_PIPELINE_FAVOR) {
            return dslContext.selectFrom(this)
                .where(CREATE_USER.eq(userId).and(PIPELINE_ID.eq(pipelineId)).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineFavorDao::class.java)
    }
}
