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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.repository.tables.TRepository
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.constant.RepositoryMessageCode.GIT_NOT_FOUND
import com.tencent.devops.repository.pojo.enums.RepositorySortEnum
import com.tencent.devops.repository.pojo.enums.RepositorySortTypeEnum
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
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
        type: ScmType
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
                    IS_DELETED
                ).values(
                    projectId,
                    userId,
                    aliasName,
                    url,
                    type.name,
                    now,
                    now,
                    false
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

    fun edit(dslContext: DSLContext, repositoryId: Long, aliasName: String, url: String) {
        val now = LocalDateTime.now()
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(ALIAS_NAME, aliasName)
                .set(URL, url)
                .set(UPDATED_TIME, now)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun countByProject(
        dslContext: DSLContext,
        projectIds: Collection<String>,
        repositoryTypes: List<ScmType>?,
        aliasName: String?,
        repositoryIds: Set<Long>?
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
        sortType: String? = null
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
                else -> REPOSITORY_ID
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

    fun delete(dslContext: DSLContext, repositoryId: Long) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(IS_DELETED, true)
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
                I18nUtil.getCodeLanMessage(messageCode = GIT_NOT_FOUND)
            )
        }
    }

    fun getByName(dslContext: DSLContext, projectId: String, repositoryName: String): TRepositoryRecord {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectFrom(this)
                .where(ALIAS_NAME.eq(repositoryName))
                .and(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .fetchAny() ?: throw NotFoundException(
                I18nUtil.getCodeLanMessage(messageCode = GIT_NOT_FOUND)
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
}
