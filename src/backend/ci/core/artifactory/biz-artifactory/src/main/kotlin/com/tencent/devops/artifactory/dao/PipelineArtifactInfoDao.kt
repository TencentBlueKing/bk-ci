/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.artifactory.dao

import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfoQuery
import com.tencent.devops.model.artifactory.tables.TPipelineArtifactInfo
import com.tencent.devops.model.artifactory.tables.records.TPipelineArtifactInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.row
import org.springframework.stereotype.Repository

/**
 * 流水线产出物元数据 DAO
 */
@Repository
class PipelineArtifactInfoDao {

    fun create(
        dslContext: DSLContext,
        artifactInfo: PipelineArtifactInfo
    ): Int {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            val record = dslContext.newRecord(this).apply {
                id = artifactInfo.id
                projectId = artifactInfo.projectId
                pipelineId = artifactInfo.pipelineId
                pipelineName = artifactInfo.pipelineName
                buildId = artifactInfo.buildId
                buildNum = artifactInfo.buildNum
                stageId = artifactInfo.stageId
                containerId = artifactInfo.containerId
                taskId = artifactInfo.taskId
                executeCount = artifactInfo.executeCount
                artifactType = artifactInfo.artifactType
                artifactName = artifactInfo.artifactName
                artifactVersion = artifactInfo.artifactVersion
                artifactUri = artifactInfo.artifactUri
                artifactRepoUrl = artifactInfo.artifactRepoUrl
                artifactDigest = artifactInfo.artifactDigest
                artifactSize = artifactInfo.artifactSize
                codeRepoUrl = artifactInfo.codeRepoUrl
                commitId = artifactInfo.commitId
                extraInfo = artifactInfo.extraInfo
                creator = artifactInfo.creator
                modifier = artifactInfo.modifier
                createTime = artifactInfo.createTime
                updateTime = artifactInfo.updateTime
            }
            return dslContext.insertInto(this)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(PIPELINE_NAME, artifactInfo.pipelineName)
                .set(ARTIFACT_DIGEST, artifactInfo.artifactDigest)
                .set(COMMIT_ID, artifactInfo.commitId)
                .set(CODE_REPO_URL, artifactInfo.codeRepoUrl)
                .set(ARTIFACT_URI, artifactInfo.artifactUri)
                .set(ARTIFACT_REPO_URL, artifactInfo.artifactRepoUrl)
                .set(ARTIFACT_SIZE, artifactInfo.artifactSize)
                .set(EXTRA_INFO, artifactInfo.extraInfo)
                .set(UPDATE_TIME, artifactInfo.updateTime)
                .set(MODIFIER, artifactInfo.modifier)
                .execute()
        }
    }

    fun searchArtifactInfo(
        dslContext: DSLContext,
        query: PipelineArtifactInfoQuery,
        page: Int,
        pageSize: Int
    ): List<TPipelineArtifactInfoRecord> {
        val conditions = buildConditions(query)
        val offset = (page - 1) * pageSize
        val ids = if (isLatestByBuild(query)) {
            selectLatestIdsByBuild(dslContext, conditions, offset, pageSize)
        } else {
            selectIdsByConditions(dslContext, conditions, offset, pageSize)
        }
        if (ids.isEmpty()) {
            return emptyList()
        }
        return selectByIds(dslContext, ids)
    }

    fun countArtifactInfo(
        dslContext: DSLContext,
        query: PipelineArtifactInfoQuery
    ): Long {
        val conditions = buildConditions(query)
        return if (isLatestByBuild(query)) {
            countLatestByBuild(dslContext, conditions)
        } else {
            countByConditions(dslContext, conditions)
        }
    }

    // 传 buildId 未传 executeCount：收敛为当次构建各制品最新一次执行
    private fun isLatestByBuild(query: PipelineArtifactInfoQuery): Boolean {
        return !query.buildId.isNullOrBlank() && query.executeCount == null
    }

    fun listByBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<TPipelineArtifactInfoRecord> {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId)
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(BUILD_ID.eq(buildId)))
                .orderBy(EXECUTE_COUNT.desc(), CREATE_TIME.desc(), ID.desc())
                .fetch()
        }
    }

    private fun buildConditions(query: PipelineArtifactInfoQuery): List<Condition> {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return buildList {
                add(PROJECT_ID.eq(query.projectId))
                if (!query.artifactType.isNullOrBlank()) {
                    add(ARTIFACT_TYPE.eq(query.artifactType))
                }
                if (!query.pipelineId.isNullOrBlank()) {
                    add(PIPELINE_ID.eq(query.pipelineId))
                }
                if (!query.buildId.isNullOrBlank()) {
                    add(BUILD_ID.eq(query.buildId))
                }
                if (query.executeCount != null) {
                    add(EXECUTE_COUNT.eq(query.executeCount))
                }
                if (!query.artifactName.isNullOrBlank()) {
                    add(ARTIFACT_NAME.eq(query.artifactName))
                }
                if (!query.artifactVersion.isNullOrBlank()) {
                    add(ARTIFACT_VERSION.eq(query.artifactVersion))
                }
            }
        }
    }

    /**
     * 覆盖索引取 ID 分页：BKM 溯源场景（类型+名称+版本等值）完全命中索引，OFFSET 丢弃行不回表
     */
    private fun selectIdsByConditions(
        dslContext: DSLContext,
        conditions: List<Condition>,
        offset: Int,
        limit: Int
    ): List<Long> {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.select(ID)
                .from(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc(), ID.desc())
                .offset(offset)
                .limit(limit)
                .fetch(ID)
        }
    }

    private fun countByConditions(
        dslContext: DSLContext,
        conditions: List<Condition>
    ): Long {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    /**
     * 当次构建最新：元组 IN 子查询收敛各制品最新一次执行（兼容 MySQL 5.7，不依赖窗口函数）
     */
    private fun selectLatestIdsByBuild(
        dslContext: DSLContext,
        conditions: List<Condition>,
        offset: Int,
        limit: Int
    ): List<Long> {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.select(ID)
                .from(this)
                .where(conditions)
                .and(latestArtifactKeys(dslContext, conditions))
                .orderBy(CREATE_TIME.desc(), ID.desc())
                .offset(offset)
                .limit(limit)
                .fetch(ID)
        }
    }

    private fun countLatestByBuild(
        dslContext: DSLContext,
        conditions: List<Condition>
    ): Long {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .and(latestArtifactKeys(dslContext, conditions))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    /**
     * 各制品（构建+类型+名称+版本）最新一次执行的元组匹配条件
     */
    private fun latestArtifactKeys(
        dslContext: DSLContext,
        conditions: List<Condition>
    ): Condition {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            val latestKeys = dslContext.select(
                BUILD_ID,
                ARTIFACT_TYPE,
                ARTIFACT_NAME,
                ARTIFACT_VERSION,
                max(EXECUTE_COUNT)
            ).from(this)
                .where(conditions)
                .groupBy(BUILD_ID, ARTIFACT_TYPE, ARTIFACT_NAME, ARTIFACT_VERSION)
            return row(BUILD_ID,
                ARTIFACT_TYPE,
                ARTIFACT_NAME,
                ARTIFACT_VERSION,
                EXECUTE_COUNT).`in`(latestKeys)
        }
    }

    /**
     * 按 ID 回表取整行，IN 不保证顺序，需重新排序
     */
    private fun selectByIds(
        dslContext: DSLContext,
        ids: List<Long>
    ): Result<TPipelineArtifactInfoRecord> {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(ids))
                .orderBy(CREATE_TIME.desc(), ID.desc())
                .fetch()
        }
    }
}
