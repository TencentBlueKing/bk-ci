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

package com.tencent.devops.stream.dao

import com.tencent.devops.model.stream.tables.TGitPipelineResource
import com.tencent.devops.model.stream.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitPipelineResourceDao {

    fun createPipeline(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipeline: StreamGitProjectPipeline,
        version: String?,
        md5: String?
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
                DIRECTORY,
                LAST_UPDATE_BRANCH,
                LAST_EDIT_MODEL_MD5
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
                pipeline.filePath.let { it.substring(0, it.indexOfLast { c -> c == '/' } + 1) },
                pipeline.lastUpdateBranch,
                md5
            ).execute()
        }
    }

    fun updatePipeline(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String,
        displayName: String,
        version: String?,
        md5: String?
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(DISPLAY_NAME, displayName)
                .set(VERSION, version)
                .set(LAST_EDIT_MODEL_MD5, md5)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun updatePipelineBuildInfo(
        dslContext: DSLContext,
        pipeline: StreamGitProjectPipeline,
        buildId: String,
        version: String?
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(LATEST_BUILD_ID, buildId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(PIPELINE_ID, pipeline.pipelineId)
                .set(VERSION, version)
                .where(GIT_PROJECT_ID.eq(pipeline.gitProjectId))
                .and(FILE_PATH.eq(pipeline.filePath))
                .execute()
        }
    }

    fun updatePipelineLastBranchAndDisplayName(
        dslContext: DSLContext,
        pipelineId: String,
        branch: String? = null,
        displayName: String? = null
    ) {

        if (branch.isNullOrEmpty() && displayName.isNullOrEmpty()) return
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val dsl = dslContext.update(this)
            if (!branch.isNullOrEmpty()) {
                dsl.set(LAST_UPDATE_BRANCH, branch)
            }
            if (!displayName.isNullOrEmpty()) {
                dsl.set(DISPLAY_NAME, displayName)
            }
            dsl.set(UPDATE_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
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

    fun getDirListByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String?
    ): Result<Record2<String, String>> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val dsl = dslContext.select(DIRECTORY, PIPELINE_ID).from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            return dsl.fetch()
        }
    }

    fun getAllPipeline(
        dslContext: DSLContext
    ): List<TGitPipelineResourceRecord> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun fixPipelineVersion(
        dslContext: DSLContext,
        pipelineId: String,
        version: String
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(VERSION, version)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun updatePipelineDisplayName(
        dslContext: DSLContext,
        pipelineId: String,
        displayName: String
    ): Int {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.update(this)
                .set(DISPLAY_NAME, displayName)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    fun getPipelines(
        dslContext: DSLContext,
        gitProjectId: Long
    ): List<TGitPipelineResourceRecord> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .fetch()
        }
    }

    fun getAppDataByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long,
        keyword: String?,
        offset: Int,
        limit: Int,
        orderBy: PipelineSortType
    ): List<TGitPipelineResourceRecord> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val dsl = dslContext.selectFrom(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(ENABLED.eq(true))
            if (!keyword.isNullOrBlank()) {
                dsl.and(DISPLAY_NAME.like("%$keyword%"))
            }
            when (orderBy) {
                PipelineSortType.NAME -> {
                    dsl.orderBy(DISPLAY_NAME)
                }
                PipelineSortType.UPDATE_TIME -> {
                    dsl.orderBy(UPDATE_TIME)
                }
                PipelineSortType.CREATE_TIME -> {
                    dsl.orderBy(CREATE_TIME)
                }
                else -> dsl.orderBy(UPDATE_TIME)
            }
            return dsl.limit(limit).offset(offset)
                .fetch()
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

    fun getMinByGitProjectId(
        dslContext: DSLContext,
        gitProjectId: Long
    ): Long {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.select(DSL.min(ID))
                .from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getPipelineIdListByProjectId(
        dslContext: DSLContext,
        gitProjectId: Long,
        minId: Long,
        limit: Long
    ): Result<out Record>? {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(GIT_PROJECT_ID.eq(gitProjectId))
            conditions.add(ID.ge(minId))
            return dslContext.select(PIPELINE_ID).from(this)
                .where(conditions)
                .orderBy(ID.asc())
                .limit(limit)
                .fetch()
        }
    }

    fun getLastUpdateBranchByIds(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineIds: Set<String>
    ): Result<Record2<String, String>> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            return dslContext.select(PIPELINE_ID, LAST_UPDATE_BRANCH).from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.`in`(pipelineIds))
                .fetch()
        }
    }

    /**
     * @return md5, displayName, ymlVersion
     */
    fun getLastEditMd5ById(
        dslContext: DSLContext,
        gitProjectId: Long,
        pipelineId: String
    ): Triple<String?, String?, String?> {
        with(TGitPipelineResource.T_GIT_PIPELINE_RESOURCE) {
            val res = dslContext.select(LAST_EDIT_MODEL_MD5, DISPLAY_NAME, VERSION).from(this)
                .where(GIT_PROJECT_ID.eq(gitProjectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetchAny()

            return Triple(res?.value1(), res?.value2(), res?.value3())
        }
    }
}
