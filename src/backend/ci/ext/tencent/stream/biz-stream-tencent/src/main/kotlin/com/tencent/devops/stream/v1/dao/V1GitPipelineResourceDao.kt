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

package com.tencent.devops.stream.v1.dao

import com.tencent.devops.model.stream.tables.TGitPipelineResource
import com.tencent.devops.model.stream.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class V1GitPipelineResourceDao {

    fun createPipeline(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipeline: StreamTriggerPipeline,
        version: String?
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.insertInto(
                this,
                PIPELINE_ID,
                GIT_PROJECT_ID,
                FILE_PATH,
                DISPLAY_NAME,
                CREATOR,
                ENABLED,
                LATEST_BUILD_ID,
                VERSION,
                CREATE_TIME,
                UPDATE_TIME,
                DIRECTORY
            ).values(
                pipeline.pipelineId,
                gitProjectId,
                pipeline.filePath,
                pipeline.displayName,
                pipeline.creator,
                pipeline.enabled,
                null,
                version,
                LocalDateTime.now(),
                LocalDateTime.now(),
                pipeline.filePath.let { it.substring(0, it.indexOfLast { c -> c == '/' } + 1) }
            ).execute()
        }
    }

    fun updatePipeline(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        displayName: String,
        version: String?
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(DISPLAY_NAME, displayName)
                .set(VERSION, version)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun updatePipelineBuildInfo(
        dslContext: DSLContext,
        pipeline: StreamTriggerPipeline,
        buildId: String,
        version: String?
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(LATEST_BUILD_ID, buildId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(PIPELINE_ID, pipeline.pipelineId)
                .set(VERSION, version)
                .where(GIT_PROJECT_ID.eq(pipeline.gitProjectId.toLong()))
                .and(FILE_PATH.eq(pipeline.filePath))
                .execute()
        }
    }

    fun getPageByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long,
        keyword: String?,
        offset: Int,
        limit: Int,
        filePath: String? = null
    ): List<TGitPipelineResourceRecord> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val dsl = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
            if (!keyword.isNullOrBlank()) {
                dsl.and(DISPLAY_NAME.like("%$keyword%"))
            }
            if (!filePath.isNullOrBlank()) {
                dsl.and(DIRECTORY.eq(filePath))
            }
            return dsl.orderBy(ENABLED.desc(), DISPLAY_NAME)
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun getAllByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long
    ): List<TGitPipelineResourceRecord> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .orderBy(ENABLED.desc(), DISPLAY_NAME)
                .fetch()
        }
    }

    fun getPipelinesInIds(
        dslContext: DSLContext,
        gitProjectId: Long?,
        pipelineIds: List<String>
    ): List<TGitPipelineResourceRecord> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val dsl = dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
            if (gitProjectId != null) {
                dsl.and(GIT_PROJECT_ID.eq(gitProjectId))
            }
            return dsl.orderBy(UPDATE_TIME.desc())
                .fetch()
        }
    }

    fun getPipelineById(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String
    ): TGitPipelineResourceRecord? {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchAny()
        }
    }

    fun getPipelineCount(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.selectCount()
                .from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun enablePipelineById(
        dslContext: DSLContext,
        pipelineId: String,
        enabled: Boolean
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(ENABLED, enabled)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun deleteByPipelineId(
        dslContext: DSLContext,
        pipelineId: String
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun getPipelineByFile(
        dslContext: DSLContext,
        gitProjectId: Long,
        filePath: String
    ): TGitPipelineResourceRecord? {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(FILE_PATH.eq(filePath))
                .fetchAny()
        }
    }
}
