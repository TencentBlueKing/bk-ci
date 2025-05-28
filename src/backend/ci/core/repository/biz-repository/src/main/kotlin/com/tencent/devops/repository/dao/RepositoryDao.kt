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
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.utils.fetchCountFix
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.TRepository
import com.tencent.devops.model.repository.tables.TRepositoryCodeGit
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode.GIT_NOT_FOUND
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.RepositorySortEnum
import com.tencent.devops.repository.pojo.enums.RepositorySortTypeEnum
import java.time.LocalDateTime
import jakarta.ws.rs.NotFoundException
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.SelectForStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
@Suppress("ALL")
class RepositoryDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        aliasName: String,
        url: String,
        type: ScmType,
        atom: Boolean? = false,
        enablePac: Boolean?,
        scmCode: String
    ): Long {
        val now = LocalDateTime.now()
        var repoId = 0L
        with(TRepository.T_REPOSITORY) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                repoId = dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    USER_ID,
                    ALIAS_NAME,
                    URL,
                    TYPE,
                    CREATED_TIME,
                    UPDATED_TIME,
                    IS_DELETED,
                    UPDATED_USER,
                    ATOM,
                    ENABLE_PAC,
                    SCM_CODE
                ).values(
                    projectId,
                    userId,
                    aliasName,
                    url,
                    type.name,
                    now,
                    now,
                    false,
                    userId,
                    atom,
                    enablePac,
                    scmCode
                )
                    .returning(REPOSITORY_ID)
                    .fetchOne()!!.repositoryId
                val hashId = HashUtil.encodeOtherLongId(repoId)
                transactionContext.update(this)
                    .set(REPOSITORY_HASH_ID, hashId)
                    .where(REPOSITORY_ID.eq(repoId))
                    .execute()
            }
        }
        return repoId
    }

    fun edit(
        dslContext: DSLContext,
        repositoryId: Long,
        aliasName: String,
        url: String,
        updateUser: String
    ) {
        val now = LocalDateTime.now()
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(ALIAS_NAME, aliasName)
                .set(URL, url)
                .set(UPDATED_TIME, now)
                .set(UPDATED_USER, updateUser)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun countByProject(
        dslContext: DSLContext,
        projectIds: Collection<String>,
        repositoryTypes: List<ScmType>?,
        aliasName: String?,
        repositoryIds: Set<Long>?,
        enablePac: Boolean? = null
    ): Long {
        with(TRepository.T_REPOSITORY) {
            val step = dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.`in`(projectIds))
                .and(IS_DELETED.eq(false))
            if (repositoryIds != null) {
                step.and(REPOSITORY_ID.`in`(repositoryIds))
            }

            if (!aliasName.isNullOrBlank()) {
                step.and(ALIAS_NAME.like("%$aliasName%"))
            }
            if (enablePac != null) {
                step.and(ENABLE_PAC.eq(enablePac))
            }
            return when (repositoryTypes) {
                null -> {
                    step.fetchOne(0, Long::class.java)!!
                }
                else -> {
                    step.and(TYPE.`in`(repositoryTypes))
                    step.fetchOne(0, Long::class.java)!!
                }
            }
        }
    }

    fun countByProjectAndAliasName(
        dslContext: DSLContext,
        projectId: String,
        excludeRepositoryId: Long,
        aliasName: String
    ): Long {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.notEqual(excludeRepositoryId))
                .and(ALIAS_NAME.eq(aliasName))
                .and(IS_DELETED.eq(false))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: Set<String>,
        repositoryType: ScmType?,
        repositoryIds: Set<Long>,
        aliasName: String
    ): Long {
        val conditions = mutableListOf<Condition>()
        with(TRepository.T_REPOSITORY) {
            if (projectId.isNotEmpty()) conditions.add(PROJECT_ID.`in`(projectId))
            if (repositoryType != null) conditions.add(TYPE.eq(repositoryType.name))
            if (repositoryIds.isNotEmpty()) conditions.add(REPOSITORY_ID.`in`(repositoryIds))
            if (aliasName.isNotEmpty()) conditions.add(ALIAS_NAME.eq(aliasName))
            conditions.add(IS_DELETED.eq(false))

            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun listByProject(
        dslContext: DSLContext,
        projectId: String,
        repositoryType:
        ScmType?
    ): Result<TRepositoryRecord> {
        with(TRepository.T_REPOSITORY) {
            return when (repositoryType) {
                null -> {
                    dslContext.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .and(IS_DELETED.eq(false))
                        .orderBy(REPOSITORY_ID.desc())
                        .fetch()
                }
                else -> {
                    dslContext.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .and(TYPE.eq(repositoryType.name))
                        .and(IS_DELETED.eq(false))
                        .orderBy(REPOSITORY_ID.desc())
                        .fetch()
                }
            }
        }
    }

    fun listRepositoryAuthorization(
        dslContext: DSLContext,
        projectId: String,
        limit: Int,
        offset: Int
    ): List<RepositoryInfo> {
        val repositoryAuthorizationQuery = buildRepositoryAuthorizationQuery(
            dslContext = dslContext,
            projectId = projectId
        )
        with(TRepository.T_REPOSITORY) {
            return dslContext.select()
                .from(repositoryAuthorizationQuery)
                .limit(limit)
                .offset(offset)
                .skipCheck()
                .fetch()
                .map {
                    RepositoryInfo(
                        repositoryId = it[REPOSITORY_ID],
                        repositoryHashId = HashUtil.encodeOtherLongId(it[REPOSITORY_ID]),
                        aliasName = it[ALIAS_NAME],
                        url = it[URL],
                        type = ScmType.valueOf(it[TYPE]),
                        createUser = it[USER_ID],
                        createdTime = it[CREATED_TIME].timestampmilli(),
                        updatedTime = it[UPDATED_TIME].timestampmilli()
                    )
                }
        }
    }

    fun countRepositoryAuthorization(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        val repositoryAuthorizationQuery = buildRepositoryAuthorizationQuery(
            dslContext = dslContext,
            projectId = projectId
        )
        return dslContext.fetchCountFix(repositoryAuthorizationQuery)
    }

    private fun buildRepositoryAuthorizationQuery(
        dslContext: DSLContext,
        projectId: String
    ): SelectForStep<out Record> {
        val tRepositoryCodeGit = TRepositoryCodeGit.T_REPOSITORY_CODE_GIT
        with(TRepository.T_REPOSITORY) {
            val codeGitQuery = dslContext.select(
                REPOSITORY_ID,
                ALIAS_NAME,
                URL,
                TYPE,
                USER_ID,
                CREATED_TIME,
                UPDATED_TIME
            )
                .from(this)
                .join(tRepositoryCodeGit)
                .on(REPOSITORY_ID.eq(tRepositoryCodeGit.REPOSITORY_ID))
                .where(IS_DELETED.eq(false))
                .and(PROJECT_ID.eq(projectId))
                .and(TYPE.eq(ScmType.CODE_GIT.name))
                .and(tRepositoryCodeGit.AUTH_TYPE.eq(RepoAuthType.OAUTH.name))

            val gitHubQuery = dslContext.select(
                REPOSITORY_ID,
                ALIAS_NAME,
                URL,
                TYPE,
                USER_ID,
                CREATED_TIME,
                UPDATED_TIME
            ).from(this)
                .where(IS_DELETED.eq(false))
                .and(PROJECT_ID.eq(projectId))
                .and(TYPE.eq(ScmType.GITHUB.name))

            return codeGitQuery.unionAll(gitHubQuery).orderBy(CREATED_TIME.desc())
        }
    }

    fun listByProject(
        dslContext: DSLContext,
        projectIds: Collection<String>,
        repositoryType: ScmType?,
        repositoryIds: Set<Long>?,
        offset: Int,
        limit: Int
    ): Result<TRepositoryRecord> {
        return with(TRepository.T_REPOSITORY) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.`in`(projectIds))
                .and(IS_DELETED.eq(false))

            if (repositoryIds != null && repositoryIds.isNotEmpty()) {
                query.and(REPOSITORY_ID.`in`(repositoryIds))
            }
            if (repositoryType != null) {
                query.and(TYPE.`in`(repositoryType.name))
            }
            query.orderBy(REPOSITORY_ID.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun listByProject(
        dslContext: DSLContext,
        projectId: String,
        repositoryTypes: List<ScmType>?,
        aliasName: String?,
        repositoryIds: Set<Long>?,
        offset: Int,
        limit: Int,
        sortBy: String? = null,
        sortType: String? = null,
        enablePac: Boolean? = null
    ): Result<TRepositoryRecord> {
        with(TRepository.T_REPOSITORY) {
            val step = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
            if (repositoryIds != null) {
                step.and(REPOSITORY_ID.`in`(repositoryIds))
            }
            if (!aliasName.isNullOrBlank()) {
                step.and(ALIAS_NAME.like("%$aliasName%"))
            }
            if (enablePac != null) {
                step.and(ENABLE_PAC.eq(enablePac))
            }

            when (repositoryTypes) {
                null -> {
                }
                else -> {
                    step.and(TYPE.`in`(repositoryTypes))
                }
            }
            val sortField = when (sortBy) {
                RepositorySortEnum.ALIAS_NAME.name -> ALIAS_NAME
                RepositorySortEnum.URL.name -> URL
                RepositorySortEnum.TYPE.name -> TYPE
                RepositorySortEnum.REPOSITORY_ID.name -> REPOSITORY_ID
                else -> UPDATED_TIME
            }

            val sort = when (sortType) {
                RepositorySortTypeEnum.ASC.name -> sortField.asc()
                RepositorySortTypeEnum.DESC.name -> sortField.desc()
                else -> sortField.desc()
            }

            return step.orderBy(sort)
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun delete(dslContext: DSLContext, repositoryId: Long, deleteAliasName: String, updateUser: String) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(IS_DELETED, true)
                .set(ALIAS_NAME, deleteAliasName)
                .set(UPDATED_USER, updateUser)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, repositoryId: Long, projectId: String? = null): TRepositoryRecord {
        with(TRepository.T_REPOSITORY) {
            val query = dslContext.selectFrom(this).where(REPOSITORY_ID.eq(repositoryId))
            if (!projectId.isNullOrBlank()) {
                query.and(PROJECT_ID.eq(projectId))
            }
            return query.and(IS_DELETED.eq(false)).fetchOne() ?: throw NotFoundException(
                I18nUtil.getCodeLanMessage(messageCode = GIT_NOT_FOUND, params = arrayOf(repositoryId.toString()))
            )
        }
    }

    fun getOrNull(dslContext: DSLContext, repositoryId: Long, projectId: String? = null): TRepositoryRecord? {
        with(TRepository.T_REPOSITORY) {
            val query = dslContext.selectFrom(this).where(REPOSITORY_ID.eq(repositoryId))
            if (!projectId.isNullOrBlank()) {
                query.and(PROJECT_ID.eq(projectId))
            }
            return query.and(IS_DELETED.eq(false)).fetchOne()
        }
    }

    fun getByName(dslContext: DSLContext, projectId: String, repositoryName: String): TRepositoryRecord {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectFrom(this)
                .where(ALIAS_NAME.eq(repositoryName))
                .and(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .fetchAny() ?: throw NotFoundException(
                I18nUtil.getCodeLanMessage(messageCode = GIT_NOT_FOUND, params = arrayOf(repositoryName))
            )
        }
    }

    fun getRepoByIds(
        dslContext: DSLContext,
        repositoryIds: List<Long>,
        checkDelete: Boolean = false
    ): Result<TRepositoryRecord>? {
        with(TRepository.T_REPOSITORY) {
            val conditions = mutableListOf(REPOSITORY_ID.`in`(repositoryIds))
            if (checkDelete) conditions.add(IS_DELETED.eq(false))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getRepoByNames(
        dslContext: DSLContext,
        repositoryNames: List<String>,
        checkDelete: Boolean = false
    ): Result<TRepositoryRecord>? {
        with(TRepository.T_REPOSITORY) {
            val conditions = mutableListOf(ALIAS_NAME.`in`(repositoryNames))
            if (checkDelete) conditions.add(IS_DELETED.eq(false))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun getAllRepo(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<Record1<Long>>? {
        with(TRepository.T_REPOSITORY) {
            return dslContext.select(REPOSITORY_ID).from(this)
                .orderBy(CREATED_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun updateHashId(
        dslContext: DSLContext,
        id: Long,
        hashId: String
    ) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(REPOSITORY_HASH_ID, hashId)
                .where(REPOSITORY_ID.eq(id))
                .and(REPOSITORY_HASH_ID.isNull)
                .execute()
        }
    }

    fun getProjectIdByGitDomain(
        dslContext: DSLContext,
        gitDomain: String,
        limit: Int,
        offset: Int
    ): List<String> {
        return with(TRepository.T_REPOSITORY) {
            dslContext.select(PROJECT_ID).from(this)
                .where(URL.like("%$gitDomain%"))
                .groupBy(PROJECT_ID)
                .limit(limit).offset(offset)
                .fetchInto(String::class.java)
        }
    }

    fun updateGitDomainByProjectId(
        dslContext: DSLContext,
        oldGitDomain: String,
        newGitDomain: String,
        projectId: String
    ): Int {
        return with(TRepository.T_REPOSITORY) {
            dslContext.update(this).set(URL, DSL.replace(URL, oldGitDomain, newGitDomain))
                .where(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun rename(
        dslContext: DSLContext,
        hashId: String,
        updateUser: String,
        projectId: String,
        newName: String
    ) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(ALIAS_NAME, newName)
                .set(UPDATED_USER, updateUser)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(REPOSITORY_HASH_ID.eq(hashId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getById(
        dslContext: DSLContext,
        repositoryId: Long
    ): TRepositoryRecord? {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectFrom(this)
                .where(
                    REPOSITORY_ID.eq(repositoryId).and(IS_DELETED.eq(false))
                )
                .fetchAny()
        }
    }

    fun updateAtomRepoFlag(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        atom: Boolean
    ) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(ATOM, atom)
                .where(REPOSITORY_ID.eq(repositoryId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getGitProjectIdByRepositoryHashId(
        dslContext: DSLContext,
        repositoryHashIdList: List<String>
    ): List<String> {
        val tRepository = TRepository.T_REPOSITORY
        with(TRepositoryCodeGit.T_REPOSITORY_CODE_GIT) {
            return dslContext.select(GIT_PROJECT_ID)
                .from(this)
                .join(tRepository).on(REPOSITORY_ID.eq(tRepository.REPOSITORY_ID))
                .where(tRepository.REPOSITORY_HASH_ID.`in`(repositoryHashIdList))
                .fetchInto(String::class.java)
        }
    }

    fun getPacRepositoryByIds(dslContext: DSLContext, repositoryIds: List<Long>): TRepositoryRecord? {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectFrom(this)
                .where(REPOSITORY_ID.`in`(repositoryIds))
                .and(ENABLE_PAC.eq(true))
                .and(IS_DELETED.eq(false))
                .fetchOne()
        }
    }

    fun enablePac(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        repositoryId: Long,
        syncStatus: String? = null
    ) {
        return with(TRepository.T_REPOSITORY) {
            val update = dslContext.update(this)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(UPDATED_USER, userId)
                .set(ENABLE_PAC, true)
            syncStatus?.let { update.set(YAML_SYNC_STATUS, syncStatus) }

            update.where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun disablePac(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        repositoryId: Long
    ) {
        return with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(ENABLE_PAC, false)
                .setNull(YAML_SYNC_STATUS)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(UPDATED_USER, userId)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun updateYamlSyncStatus(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        syncStatus: String
    ) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(YAML_SYNC_STATUS, syncStatus)
                .where(REPOSITORY_ID.eq(repositoryId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun updateStoreRepoProject(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        repositoryId: Long
    ) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(PROJECT_ID, projectId)
                .set(USER_ID, userId)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun countByScmCode(
        dslContext: DSLContext,
        scmCode: String
    ): Long {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectCount().from(this)
                .where(SCM_CODE.eq(scmCode))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun countByScmCodes(
        dslContext: DSLContext,
        scmCodes: List<String>
    ): Map<String, Long> {
        return with(TRepository.T_REPOSITORY) {
            dslContext.select(SCM_CODE, DSL.count())
                .from(this)
                .where(SCM_CODE.`in`(scmCodes))
                .fetch().map {
                    Pair(it.value1(), it.value2().toLong())
                }.toMap()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String?,
        repositoryTypes: List<String>?,
        repoHashId: String?,
        nullScmCode: Boolean? = null,
        limit: Int,
        offset: Int
    ): List<TRepositoryRecord> {
        return with(TRepository.T_REPOSITORY) {
            val conditions = mutableListOf(
                IS_DELETED.eq(false)
            )
            if (!repositoryTypes.isNullOrEmpty()) {
                conditions.add(TYPE.`in`(repositoryTypes))
            }
            if (!projectId.isNullOrBlank()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (!repoHashId.isNullOrBlank()) {
                conditions.add(REPOSITORY_HASH_ID.eq(repoHashId))
            }
            if (nullScmCode == true) {
                conditions.add(SCM_CODE.isNull())
            }
            dslContext.selectFrom(this)
                    .where(conditions)
                    .orderBy(PROJECT_ID, REPOSITORY_ID)
                    .limit(limit).offset(offset)
                    .fetch()
        }
    }

    fun updateScmCode(
        dslContext: DSLContext,
        repositoryId: Set<Long>
    ) {
        if (repositoryId.isEmpty()) return
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(SCM_CODE, field(TYPE))
                .where(REPOSITORY_ID.`in`(repositoryId))
                .execute()
        }
    }
}
