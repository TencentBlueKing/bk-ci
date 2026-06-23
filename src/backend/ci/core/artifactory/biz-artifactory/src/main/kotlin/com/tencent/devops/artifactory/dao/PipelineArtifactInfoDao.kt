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

import com.tencent.devops.model.artifactory.tables.TPipelineArtifactInfo
import com.tencent.devops.model.artifactory.tables.records.TPipelineArtifactInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Result
import org.jooq.Select
import org.springframework.stereotype.Repository

/**
 * 流水线产出物元数据 DAO
 */
@Repository
@Suppress("ALL")
class PipelineArtifactInfoDao {

    private companion object {
        val TABLE = TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO
    }

    /**
     * 保存产出物元数据
     */
    fun create(
        dslContext: DSLContext,
        artifactInfo: Map<String, Any?>
    ): Long {
        val record = dslContext.insertInto(
            TABLE,
            TABLE.PROJECT_ID,
            TABLE.PIPELINE_ID,
            TABLE.PIPELINE_NAME,
            TABLE.BUILD_ID,
            TABLE.BUILD_NUM,
            TABLE.STAGE_ID,
            TABLE.CONTAINER_ID,
            TABLE.TASK_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.ARTIFACT_TYPE,
            TABLE.ARTIFACT_NAME,
            TABLE.ARTIFACT_VERSION,
            TABLE.ARTIFACT_URI,
            TABLE.ARTIFACT_REPO_URL,
            TABLE.ARTIFACT_DIGEST,
            TABLE.ARTIFACT_SIZE,
            TABLE.CODE_REPO_URL,
            TABLE.COMMIT_ID,
            TABLE.EXTRA_INFO,
            TABLE.CREATOR,
            TABLE.MODIFIER
        )
            .values(
                artifactInfo["projectId"] as String,
                artifactInfo["pipelineId"] as String,
                artifactInfo["pipelineName"] as? String,
                artifactInfo["buildId"] as String,
                artifactInfo["buildNum"] as? Int,
                artifactInfo["stageId"] as? String ?: "",
                artifactInfo["containerId"] as? String ?: "",
                artifactInfo["taskId"] as? String ?: "",
                artifactInfo["executeCount"] as? Int ?: 1,
                artifactInfo["artifactType"] as String,
                artifactInfo["artifactName"] as String,
                artifactInfo["artifactVersion"] as? String ?: "",
                artifactInfo["artifactUri"] as? String,
                artifactInfo["artifactRepoUrl"] as? String,
                artifactInfo["artifactDigest"] as? String,
                artifactInfo["artifactSize"] as? Long,
                artifactInfo["codeRepoUrl"] as? String,
                artifactInfo["commitId"] as? String ?: "",
                artifactInfo["extraInfo"] as? String,
                artifactInfo["creator"] as? String ?: "system",
                artifactInfo["modifier"] as? String ?: "system"
            )
            .returning(TABLE.ID)
            .fetchOne()

        return record?.value1() ?: 0L
    }

    /**
     * 根据产出物信息查询元数据
     */
    fun getByArtifact(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String,
        artifactVersion: String
    ): TPipelineArtifactInfoRecord? {
        return with(TABLE) {
            val conditions = mutableListOf<Condition>(
                PROJECT_ID.eq(projectId),
                ARTIFACT_TYPE.eq(artifactType),
                ARTIFACT_NAME.eq(artifactName),
                ARTIFACT_VERSION.eq(artifactVersion)
            )
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .fetchAny()
        }
    }

    /**
     * 根据构建信息查询产出物列表
     */
    fun listByBuild(
        dslContext: DSLContext,
        pipelineId: String,
        buildId: String
    ): Result<TPipelineArtifactInfoRecord> {
        return with(TABLE) {
            dslContext.selectFrom(this)
                .where(
                    PIPELINE_ID.eq(pipelineId)
                        .and(BUILD_ID.eq(buildId))
                )
                .fetch()
        }
    }

    /**
     * 根据构建 ID 列表删除元数据（数据清理用）
     */
    fun deleteByBuildIds(
        dslContext: DSLContext,
        buildIds: List<String>
    ): Int {
        if (buildIds.isEmpty()) {
            return 0
        }
        return with(TABLE) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }

    /**
     * 根据项目和时间范围删除元数据（数据清理用）
     */
    fun deleteByProjectAndTime(
        dslContext: DSLContext,
        projectId: String,
        beforeTime: java.time.LocalDateTime
    ): Int {
        return with(TABLE) {
            dslContext.deleteFrom(this)
                .where(
                    PROJECT_ID.eq(projectId)
                        .and(CREATE_TIME.lessOrEqual(beforeTime))
                )
                .execute()
        }
    }

    /**
     * 统计项目的产出物数量
     */
    fun countByProject(
        dslContext: DSLContext,
        projectId: String
    ): Long {
        return with(TABLE) {
            dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    /**
     * 分页查询项目的产出物
     */
    fun listByProject(
        dslContext: DSLContext,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Result<TPipelineArtifactInfoRecord> {
        return with(TABLE) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.desc())
                .limit((page - 1) * pageSize, pageSize)
                .fetch()
        }
    }
}
