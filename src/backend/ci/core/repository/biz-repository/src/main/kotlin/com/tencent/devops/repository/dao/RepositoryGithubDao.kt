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

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.model.repository.tables.TRepository
import com.tencent.devops.model.repository.tables.TRepositoryCodeGitlab
import com.tencent.devops.model.repository.tables.TRepositoryGithub
import com.tencent.devops.model.repository.tables.records.TRepositoryGithubRecord
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.RepoOauthRefVo
import com.tencent.devops.repository.pojo.UpdateRepositoryInfoRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import jakarta.ws.rs.NotFoundException
import org.jooq.Condition

@Repository
class RepositoryGithubDao {
    fun create(
        dslContext: DSLContext,
        repositoryId: Long,
        projectName: String,
        userName: String,
        gitProjectId: Long?
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryGithub.T_REPOSITORY_GITHUB) {
            dslContext.insertInto(
                this,
                REPOSITORY_ID,
                PROJECT_NAME,
                USER_NAME,
                CREATED_TIME,
                UPDATED_TIME,
                GIT_PROJECT_ID
            )
                .values(
                    repositoryId,
                    projectName,
                    userName,
                    now,
                    now,
                    gitProjectId
                ).execute()
        }
    }

    fun getOrNull(dslContext: DSLContext, repositoryId: Long): TRepositoryGithubRecord? {
        with(TRepositoryGithub.T_REPOSITORY_GITHUB) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.eq(repositoryId))
                .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, repositoryId: Long): TRepositoryGithubRecord {
        return getOrNull(dslContext, repositoryId) ?: throw NotFoundException("Github repository does not exists")
    }

    fun edit(
        dslContext: DSLContext,
        repositoryId: Long,
        projectName: String,
        userName: String,
        gitProjectId: Long?
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryGithub.T_REPOSITORY_GITHUB) {
            val updateStep = dslContext.update(this)
                .set(PROJECT_NAME, projectName)
                .set(USER_NAME, userName)
                .set(UPDATED_TIME, now)
            if (gitProjectId != null) {
                updateStep.set(GIT_PROJECT_ID, gitProjectId)
            }
            updateStep.where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun updateRepositoryInfo(
        dslContext: DSLContext,
        repositoryId: Long,
        updateRepositoryInfoRequest: UpdateRepositoryInfoRequest
    ) {
        with(TRepositoryCodeGitlab.T_REPOSITORY_CODE_GITLAB) {
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
            baseStep.set(UPDATED_TIME, LocalDateTime.now())
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, repositoryIds: Set<Long>): Result<TRepositoryGithubRecord>? {
        with(TRepositoryGithub.T_REPOSITORY_GITHUB) {
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
    ): Result<TRepositoryGithubRecord>? {
        with(TRepositoryGithub.T_REPOSITORY_GITHUB) {
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
        with(TRepositoryGithub.T_REPOSITORY_GITHUB) {
            val conditions = mutableListOf(
                REPOSITORY_ID.eq(id)
            )
            dslContext.update(this)
                .set(GIT_PROJECT_ID, gitProjectId)
                .where(conditions)
                .execute()
        }
    }

    fun countOauthRepo(
        dslContext: DSLContext,
        userId: String
    ): Long {
        val t1 = TRepository.T_REPOSITORY
        val t2 = TRepositoryGithub.T_REPOSITORY_GITHUB
        return dslContext.selectCount()
            .from(t1)
            .leftJoin(t2)
            .on(t1.REPOSITORY_ID.eq(t2.REPOSITORY_ID))
            .where(
                listOf(
                    t1.IS_DELETED.eq(false),
                    t1.TYPE.eq(ScmType.GITHUB.name),
                    t2.USER_NAME.eq(userId)
                )
            )
            .fetchOne(0, Long::class.java)!!
    }

    fun listOauthRepo(
        dslContext: DSLContext,
        userId: String,
        limit: Int,
        offset: Int
    ): List<RepoOauthRefVo> {
        val t1 = TRepository.T_REPOSITORY
        val t2 = TRepositoryGithub.T_REPOSITORY_GITHUB
        return dslContext.select(t1.ALIAS_NAME, t1.URL, t1.PROJECT_ID, t1.REPOSITORY_HASH_ID)
            .from(t1)
            .leftJoin(t2)
            .on(t1.REPOSITORY_ID.eq(t2.REPOSITORY_ID))
            .where(
                listOf(
                    t1.IS_DELETED.eq(false),
                    t1.TYPE.eq(ScmType.GITHUB.name),
                    t2.USER_NAME.eq(userId)
                )
            )
            .limit(limit)
            .offset(offset)
            .fetch()
            .map {
                RepoOauthRefVo(
                    aliasName = it.get(0).toString(),
                    url = it.get(1).toString(),
                    projectId = it.get(2).toString(),
                    hashId = it.get(3).toString()
                )
            }
    }

    fun listByCondition(
        dslContext: DSLContext,
        repoCondition: RepoCondition,
        limit: Int,
        offset: Int
    ): List<GithubRepository> {
        val t1 = TRepository.T_REPOSITORY
        val t2 = TRepositoryGithub.T_REPOSITORY_GITHUB
        val condition = buildCondition(t1, t2, repoCondition)

        return dslContext.select(
            t1.ALIAS_NAME,
            t1.URL,
            t2.PROJECT_NAME,
            t2.USER_NAME,
            t1.PROJECT_ID,
            t1.REPOSITORY_ID,
            t2.GIT_PROJECT_ID,
            t1.ENABLE_PAC,
            t1.YAML_SYNC_STATUS,
            t1.SCM_CODE
        ).from(t1)
                .leftJoin(t2)
                .on(t1.REPOSITORY_ID.eq(t2.REPOSITORY_ID))
                .where(condition)
                .limit(limit)
                .offset(offset)
                .map {
                    GithubRepository(
                        aliasName = it.value1(),
                        url = it.value2(),
                        credentialId = "",
                        projectName = it.value3(),
                        userName = it.value4(),
                        projectId = it.value5(),
                        repoHashId = HashUtil.encodeOtherLongId(it.value6()),
                        gitProjectId = it.value7(),
                        enablePac = it.value8(),
                        yamlSyncStatus = it.value9(),
                        scmCode = it.value10() ?: ScmType.GITHUB.name
                    )
                }
    }

    fun countByCondition(
        dslContext: DSLContext,
        repoCondition: RepoCondition
    ): Long {
        val t1 = TRepository.T_REPOSITORY
        val t2 = TRepositoryGithub.T_REPOSITORY_GITHUB
        val condition = buildCondition(t1, t2, repoCondition)
        return dslContext.selectCount()
                .from(t1)
                .leftJoin(t2)
                .on(t1.REPOSITORY_ID.eq(t2.REPOSITORY_ID))
                .where(condition)
                .fetchOne(0, Long::class.java) ?: 0L
    }

    private fun buildCondition(
        t1: TRepository,
        t2: TRepositoryGithub,
        repoCondition: RepoCondition
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.IS_DELETED.eq(false))
        with(repoCondition) {
            if (!projectId.isNullOrEmpty()) {
                conditions.add(t1.PROJECT_ID.eq(projectId))
            }
            if (!repoIds.isNullOrEmpty()) {
                conditions.add(t1.REPOSITORY_ID.`in`(repoIds))
            }
            type?.let { conditions.add(t1.TYPE.eq(it.name)) }
            if (!projectName.isNullOrEmpty()) {
                conditions.add(t2.PROJECT_NAME.eq(projectName))
            }
            if (!oauthUserId.isNullOrEmpty()) {
                conditions.add(t2.USER_NAME.eq(oauthUserId))
            }
            if (!scmCode.isNullOrEmpty()) {
                conditions.add(t1.SCM_CODE.eq(scmCode))
            }
            enablePac?.let {
                conditions.add(t1.ENABLE_PAC.eq(enablePac))
            }
        }
        return conditions
    }
}
