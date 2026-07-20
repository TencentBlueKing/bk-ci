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
import com.tencent.devops.model.artifactory.tables.TPipelineArtifactInfo
import com.tencent.devops.model.artifactory.tables.records.TPipelineArtifactInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
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
                .set(EXECUTE_COUNT, artifactInfo.executeCount)
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

    fun getByArtifact(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String,
        artifactVersion: String,
        executeCount: Int? = null,
        buildId: String? = null,
        taskId: String? = null
    ): TPipelineArtifactInfoRecord? {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            val conditions = buildList<Condition> {
                add(PROJECT_ID.eq(projectId))
                add(ARTIFACT_TYPE.eq(artifactType))
                if (!pipelineId.isNullOrBlank()) {
                    add(PIPELINE_ID.eq(pipelineId))
                }
                if (!buildId.isNullOrBlank()) {
                    add(BUILD_ID.eq(buildId))
                }
                if (executeCount != null) {
                    add(EXECUTE_COUNT.eq(executeCount))
                }
                if (!taskId.isNullOrBlank()) {
                    add(TASK_ID.eq(taskId))
                }
                add(ARTIFACT_NAME.eq(artifactName))
                add(ARTIFACT_VERSION.eq(artifactVersion))
            }
                return dslContext.selectFrom(this)
                    .where(conditions)
                    .orderBy(EXECUTE_COUNT.desc(), CREATE_TIME.desc())
                    .fetchOne()
        }
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
                .fetch()
        }
    }
}
