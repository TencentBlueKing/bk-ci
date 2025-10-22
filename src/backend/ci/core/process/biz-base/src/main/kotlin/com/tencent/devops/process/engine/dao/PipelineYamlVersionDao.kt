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
 *
 */

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.model.process.tables.TPipelineYamlVersion
import com.tencent.devops.model.process.tables.records.TPipelineYamlVersionRecord
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 流水线与代码库yml文件关联表
 */
@Repository
class PipelineYamlVersionDao {

    fun save(
        dslContext: DSLContext,
        id: Long,
        projectId: String,
        repoHashId: String,
        filePath: String,
        blobId: String,
        commitId: String,
        ref: String?,
        pipelineId: String,
        version: Int,
        commitTime: LocalDateTime,
        userId: String
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                REPO_HASH_ID,
                FILE_PATH,
                REF,
                COMMIT_ID,
                COMMIT_TIME,
                BLOB_ID,
                PIPELINE_ID,
                VERSION,
                BRANCH_ACTION,
                CREATOR,
                CREATE_TIME
            ).values(
                id,
                projectId,
                repoHashId,
                filePath,
                ref,
                commitId,
                commitTime,
                blobId,
                pipelineId,
                version,
                BranchVersionAction.ACTIVE.name,
                userId,
                now
            ).execute()
        }
    }

    fun getPipelineYamlVersion(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String? = null,
        commitId: String? = null,
        blobId: String? = null,
        branchAction: String? = null
    ): PipelineYamlVersion? {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
                .let { if (ref != null) it.and(REF.eq(ref)) else it }
                .let { if (commitId != null) it.and(COMMIT_ID.eq(ref)) else it }
                .let { if (blobId != null) it.and(BLOB_ID.eq(blobId)) else it }
                .let { if (branchAction != null) it.and(BRANCH_ACTION.eq(branchAction)) else it }
                .orderBy(COMMIT_TIME.desc())
                .limit(1)
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    fun getPipelineYamlVersion(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVersion? {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            val record = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
            return record?.let { convert(it) }
        }
    }

    /**
     * 获取分支列表
     */
    fun listRef(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        branchAction: String,
        excludeRef: String? = null
    ): List<String> {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            val query = dslContext.select(REF).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
                .and(BRANCH_ACTION.eq(branchAction))
                .let { if (excludeRef != null) it.and(REF.notEqual(excludeRef)) else it }

            return query.groupBy(REF).fetch().map { it.value1() }
        }
    }

    fun updateBranchAction(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        branchAction: String
    ) {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            dslContext.update(this)
                .set(BRANCH_ACTION, branchAction)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
                .and(REF.eq(ref))
                .execute()
        }
    }

    fun deleteAll(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String
    ) {
        with(TPipelineYamlVersion.T_PIPELINE_YAML_VERSION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH.eq(filePath))
                .execute()
        }
    }

    fun convert(record: TPipelineYamlVersionRecord): PipelineYamlVersion {
        return with(record) {
            PipelineYamlVersion(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId,
                commitId = commitId,
                ref = ref,
                pipelineId = pipelineId,
                version = version
            )
        }
    }
}
