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

import com.tencent.devops.model.process.tables.TPipelineYamlBranchFile
import com.tencent.devops.model.process.tables.records.TPipelineYamlBranchFileRecord
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineYamlBranchFileDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String,
        commitId: String,
        blobId: String,
        commitTime: LocalDateTime
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                REPO_HASH_ID,
                BRANCH,
                FILE_PATH,
                FILE_PATH_MD5,
                CREATE_TIME,
                UPDATE_TIME,
                COMMIT_ID,
                BLOB_ID,
                COMMIT_TIME
            ).values(
                projectId,
                repoHashId,
                branch,
                filePath,
                DigestUtils.md5Hex(filePath),
                now,
                now,
                commitId,
                blobId,
                commitTime
            ).onDuplicateKeyUpdate()
                .set(UPDATE_TIME, now)
                .set(COMMIT_ID, commitId)
                .set(BLOB_ID, blobId)
                .set(COMMIT_TIME, commitTime)
                .execute()
        }
    }

    fun getAllFilePath(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        branch: String
    ): List<String> {
        return with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.select(FILE_PATH).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(BRANCH.eq(branch))
                .fetch(0, String::class.java)
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String
    ): TPipelineYamlBranchFileRecord? {
        return with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(BRANCH.eq(branch))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .fetchOne()
        }
    }

    fun listBranch(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        filePath: String
    ): List<String> {
        return with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.select(BRANCH).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .fetch(0, String::class.java)
        }
    }

    fun deleteFile(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String
    ) {
        with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(BRANCH.eq(branch))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .execute()
        }
    }

    fun softDelete(
        dslContext: DSLContext,
        projectId: String,
        repoHashId: String,
        branch: String,
        filePath: String
    ) {
        val now = LocalDateTime.now()
        with(TPipelineYamlBranchFile.T_PIPELINE_YAML_BRANCH_FILE) {
            dslContext.update(this)
                .set(DELETED, true)
                .set(UPDATE_TIME, now)
                .where(PROJECT_ID.eq(projectId))
                .and(REPO_HASH_ID.eq(repoHashId))
                .and(BRANCH.eq(branch))
                .and(FILE_PATH_MD5.eq(DigestUtils.md5Hex(filePath)))
                .execute()
        }
    }
}
