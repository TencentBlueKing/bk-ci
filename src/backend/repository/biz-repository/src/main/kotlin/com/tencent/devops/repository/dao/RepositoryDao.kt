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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.dao

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.model.repository.tables.TRepository
import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
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
        with(TRepository.T_REPOSITORY) {
            val record = dslContext.insertInto(
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
                .fetchOne()
            return record.repositoryId
        }
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
        projectId: String,
        repositoryType: ScmType?,
        repositoryIds: Set<Long>
    ): Long {
        return with(TRepository.T_REPOSITORY) {
            val query = dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))

            if (repositoryType != null) {
                query.and(TYPE.eq(repositoryType.name))
            }
            if (repositoryIds.isNotEmpty()) {
                query.and(REPOSITORY_ID.`in`(repositoryIds))
            }
            query.fetchOne(0, Long::class.java)
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
                .fetchOne(0, Long::class.java)
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
                .fetchOne(0, Long::class.java)
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
        projectId: String,
        repositoryType: ScmType?,
        repositoryIds: Set<Long>,
        offset: Int,
        limit: Int
    ): Result<TRepositoryRecord> {
        return with(TRepository.T_REPOSITORY) {
            val query = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))

            if (repositoryIds.isNotEmpty()) {
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

    fun delete(dslContext: DSLContext, repositoryId: Long) {
        with(TRepository.T_REPOSITORY) {
            dslContext.update(this)
                .set(IS_DELETED, true)
                .where(REPOSITORY_ID.eq(repositoryId))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, repositoryId: Long, projectId: String): TRepositoryRecord {
        with(TRepository.T_REPOSITORY) {
            val query = dslContext.selectFrom(this).where(REPOSITORY_ID.eq(repositoryId))
            if (projectId.isNotBlank()) {
                query.and(PROJECT_ID.eq(projectId))
            }
            return query.and(IS_DELETED.eq(false)).fetchOne() ?: throw NotFoundException("代码库不存在")
        }
    }

    fun getByName(dslContext: DSLContext, projectId: String, repositoryName: String): TRepositoryRecord {
        with(TRepository.T_REPOSITORY) {
            return dslContext.selectFrom(this)
                .where(ALIAS_NAME.eq(repositoryName))
                .and(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .fetchOne() ?: throw NotFoundException("代码库${repositoryName}不存在")
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
}