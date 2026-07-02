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
import com.tencent.devops.artifactory.pojo.artifact.PipelineArtifactInfo
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
            }
            return record.store()
        }
    }

    fun getByArtifact(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String?,
        artifactType: String,
        artifactName: String?,
        artifactVersion: String?
    ): TPipelineArtifactInfoRecord? {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            val conditions = mutableListOf<Condition>(
                PROJECT_ID.eq(projectId),
                ARTIFACT_TYPE.eq(artifactType)
            )
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!artifactName.isNullOrBlank()) {
                conditions.add(ARTIFACT_NAME.eq(artifactName))
            }
            if (!artifactVersion.isNullOrBlank()) {
                conditions.add(ARTIFACT_VERSION.eq(artifactVersion))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchAny()
        }
    }

    fun listByBuild(
        dslContext: DSLContext,
        pipelineId: String,
        buildId: String
    ): Result<TPipelineArtifactInfoRecord> {
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId).and(BUILD_ID.eq(buildId)))
                .fetch()
        }
    }

    fun deleteByBuildIds(
        dslContext: DSLContext,
        buildIds: List<String>
    ): Int {
        if (buildIds.isEmpty()) {
            return 0
        }
        with(TPipelineArtifactInfo.T_PIPELINE_ARTIFACT_INFO) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .execute()
        }
    }
}
