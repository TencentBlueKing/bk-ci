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

package com.tencent.devops.repository.dao

import com.tencent.devops.model.repository.tables.TRepositoryCodeGit
import com.tencent.devops.model.repository.tables.TRepositoryCodeGitlab
import com.tencent.devops.model.repository.tables.records.TRepositoryCodeGitlabRecord
import com.tencent.devops.repository.pojo.UpdateRepositoryInfoRequest
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class RepositoryCodeGitLabDao {
    fun create(
        dslContext: DSLContext,
        repositoryId: Long,
        projectName: String,
        userName: String,
        privateToken: String,
        authType: RepoAuthType?,
        gitProjectId: Long
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
            dslContext.insertInto(
                this,
                REPOSITORY_ID,
                PROJECT_NAME,
                USER_NAME,
                CREDENTIAL_ID,
                CREATED_TIME,
                UPDATED_TIME,
                AUTH_TYPE,
                GIT_PROJECT_ID
            )
                .values(
                    repositoryId,
                    projectName,
                    userName,
                    privateToken,
                    now,
                    now,
                    authType?.name,
                    gitProjectId
                ).execute()
        }
    }

    fun getOrNull(dslContext: DSLContext, repositoryId: Long): TRepositoryCodeGitlabRecord? {
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.eq(repositoryId))
                .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, repositoryId: Long): TRepositoryCodeGitlabRecord {
        return getOrNull(dslContext, repositoryId) ?: throw NotFoundException("Git repository does not exists")
    }

    fun edit(
        dslContext: DSLContext,
        repositoryId: Long,
        projectName: String,
        userName: String,
        credentialId: String,
        gitProjectId: Long
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
            with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
                dslContext.update(this)
                val updateSetStep = dslContext.update(this)
                    .set(PROJECT_NAME, projectName)
                    .set(USER_NAME, userName)
                    .set(CREDENTIAL_ID, credentialId)
                    .set(UPDATED_TIME, now)
                if (gitProjectId >= 0) {
                    updateSetStep.set(GIT_PROJECT_ID, gitProjectId)
                }
                updateSetStep.where(REPOSITORY_ID.eq(repositoryId))
                    .execute()
            }
        }
    }

    fun updateRepositoryInfo(
        dslContext: DSLContext,
        repositoryId: Long,
        updateRepositoryInfoRequest: UpdateRepositoryInfoRequest
    ) {
        with(TRepositoryCodeGit.T_REPOSITORY_CODE_GIT) {
            val baseStep = dslContext.update(this)
            if (!updateRepositoryInfoRequest.projectName.isNullOrEmpty()) {
                baseStep.set(PROJECT_NAME, updateRepositoryInfoRequest.projectName)
            }
            if (!updateRepositoryInfoRequest.userId.isNullOrEmpty()) {
                baseStep.set(USER_NAME, updateRepositoryInfoRequest.userId)
            }
            if (!updateRepositoryInfoRequest.credentialId.isNullOrEmpty()) {
                baseStep.set(CREDENTIAL_ID, updateRepositoryInfoRequest.credentialId)
            }
            if (null != updateRepositoryInfoRequest.authType) {
                baseStep.set(AUTH_TYPE, updateRepositoryInfoRequest.authType!!.name)
            }
            baseStep.set(UPDATED_TIME, LocalDateTime.now())
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, repositoryIds: Set<Long>): Result<TRepositoryCodeGitlabRecord>? {
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.`in`(repositoryIds))
                .fetch()
        }
    }

    /**
     * 分页查询
     */
    fun getAllRepo(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<TRepositoryCodeGitlabRecord>? {
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
            return dslContext.selectFrom(this)
                .orderBy(CREATED_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateGitProjectId(
        dslContext: DSLContext,
        id: Long,
        gitProjectId: Long
    ) {
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
            val conditions = mutableListOf(
                REPOSITORY_ID.eq(id),
                GIT_PROJECT_ID.le(0)
            )
            dslContext.update(this)
                .set(GIT_PROJECT_ID, gitProjectId)
                .where(conditions)
                .execute()
        }
    }
}
