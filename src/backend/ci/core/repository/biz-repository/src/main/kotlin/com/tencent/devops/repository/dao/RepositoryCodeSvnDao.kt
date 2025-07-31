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

package com.tencent.devops.repository.dao

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.model.repository.tables.TRepository
import com.tencent.devops.model.repository.tables.TRepositoryCodeSvn
import com.tencent.devops.model.repository.tables.records.TRepositoryCodeSvnRecord
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.UpdateRepositoryInfoRequest
import com.tencent.devops.scm.enums.CodeSvnRegion
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import jakarta.ws.rs.NotFoundException
import org.jooq.Condition

@Repository
@Suppress("ALL")
class RepositoryCodeSvnDao {
    fun create(
        dslContext: DSLContext,
        repositoryId: Long,
        region: CodeSvnRegion?,
        projectName: String,
        userName: String,
        credentialId: String,
        svnType: String?,
        credentialType: String
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            dslContext.insertInto(
                this,
                REPOSITORY_ID,
                REGION,
                PROJECT_NAME,
                USER_NAME,
                CREDENTIAL_ID,
                CREATED_TIME,
                UPDATED_TIME,
                SVN_TYPE,
                CREDENTIAL_TYPE
            )
                .values(
                    repositoryId,
                    region?.name ?: "",
                    projectName,
                    userName,
                    credentialId,
                    now,
                    now,
                    svnType ?: "",
                    credentialType
                ).execute()
        }
    }

    private fun getOrNull(dslContext: DSLContext, repositoryId: Long): TRepositoryCodeSvnRecord? {
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.eq(repositoryId))
                .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, repositoryId: Long): TRepositoryCodeSvnRecord {
        return getOrNull(dslContext, repositoryId) ?: throw NotFoundException("Svn repository does not exists")
    }

    fun edit(
        dslContext: DSLContext,
        repositoryId: Long,
        region: CodeSvnRegion?,
        projectName: String,
        userName: String,
        credentialId: String,
        svnType: String?,
        credentialType: String
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            dslContext.update(this)
                .set(REGION, region?.name ?: "")
                .set(PROJECT_NAME, projectName)
                .set(USER_NAME, userName)
                .set(UPDATED_TIME, now)
                .set(CREDENTIAL_ID, credentialId)
                .set(SVN_TYPE, svnType ?: "")
                .set(CREDENTIAL_TYPE, credentialType)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        repositoryIds: Set<Long>
    ): Result<TRepositoryCodeSvnRecord> {
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.`in`(repositoryIds))
                .fetch()
        }
    }

    fun updateRepositoryInfo(
        dslContext: DSLContext,
        repositoryId: Long,
        updateRepositoryInfoRequest: UpdateRepositoryInfoRequest
    ) {
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
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
            if (!updateRepositoryInfoRequest.svnType.isNullOrEmpty()) {
                baseStep.set(SVN_TYPE, updateRepositoryInfoRequest.svnType)
            }
            baseStep.set(UPDATED_TIME, LocalDateTime.now())
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun updateCredentialType(
        dslContext: DSLContext,
        repositoryId: Long,
        credentialType: String
    ): Int {
        with(TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN) {
            return dslContext.update(this)
                .set(CREDENTIAL_TYPE, credentialType)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun listByCondition(
        dslContext: DSLContext,
        repoCondition: RepoCondition,
        limit: Int,
        offset: Int
    ): List<CodeSvnRepository> {
        val t1 = TRepository.T_REPOSITORY
        val t2 = TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN
        val condition = buildCondition(t1, t2, repoCondition)

        return dslContext.select(
            t1.ALIAS_NAME,
            t1.URL,
            t2.CREDENTIAL_ID,
            t2.PROJECT_NAME,
            t2.USER_NAME,
            t2.SVN_TYPE,
            t1.PROJECT_ID,
            t1.REPOSITORY_ID,
            t1.ENABLE_PAC,
            t1.YAML_SYNC_STATUS,
            t1.SCM_CODE,
            t2.CREDENTIAL_TYPE
        ).from(t1)
                .leftJoin(t2)
                .on(t1.REPOSITORY_ID.eq(t2.REPOSITORY_ID))
                .where(condition)
                .limit(limit)
                .offset(offset)
                .map {
                    CodeSvnRepository(
                        aliasName = it.value1(),
                        url = it.value2(),
                        credentialId = it.value3(),
                        projectName = it.value4(),
                        userName = it.value5(),
                        svnType = it.value6(),
                        projectId = it.value7(),
                        repoHashId = HashUtil.encodeOtherLongId(it.value8()),
                        enablePac = it.value9(),
                        yamlSyncStatus = it.value10(),
                        scmCode = it.value11() ?: ScmType.SCM_SVN.name,
                        credentialType = it.value12()
                    )
                }
    }

    fun countByCondition(
        dslContext: DSLContext,
        repoCondition: RepoCondition
    ): Long {
        val t1 = TRepository.T_REPOSITORY
        val t2 = TRepositoryCodeSvn.T_REPOSITORY_CODE_SVN
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
        t2: TRepositoryCodeSvn,
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
            svnType?.let { t2.SVN_TYPE.eq(it) }
            if (!scmCode.isNullOrEmpty()) {
                conditions.add(t1.SCM_CODE.eq(scmCode))
            }
        }
        return conditions
    }
}
