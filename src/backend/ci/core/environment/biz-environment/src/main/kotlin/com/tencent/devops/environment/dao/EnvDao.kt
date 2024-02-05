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

package com.tencent.devops.environment.dao

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.model.environment.tables.TEnv
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.jooq.impl.DSL

@Repository
@Suppress("ALL")
class EnvDao {
    fun get(dslContext: DSLContext, projectId: String, envId: Long): TEnvRecord {
        return getOrNull(dslContext, projectId, envId)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_NOT_EXISTS)
    }

    fun getByEnvName(dslContext: DSLContext, projectId: String, envName: String): TEnvRecord? {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_NAME.eq(envName))
                .and(IS_DELETED.eq(false))
                .fetchOne()
        }
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, envId: Long): TEnvRecord? {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(ENV_ID.eq(envId))
                .and(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        envName: String,
        envDesc: String,
        envType: String,
        envVars: String
    ): Long {
        val now = LocalDateTime.now()
        var envId = 0L
        with(TEnv.T_ENV) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                envId = transactionContext.insertInto(
                    this,
                    PROJECT_ID,
                    ENV_NAME,
                    ENV_DESC,
                    ENV_TYPE,
                    ENV_VARS,
                    CREATED_USER,
                    UPDATED_USER,
                    CREATED_TIME,
                    UPDATED_TIME,
                    IS_DELETED
                ).values(
                    projectId,
                    envName,
                    envDesc,
                    envType,
                    envVars,
                    userId,
                    userId,
                    now,
                    now,
                    false
                ).returning(ENV_ID).fetchOne()!!.envId
                val hashId = HashUtil.encodeLongId(envId)
                transactionContext.update(this)
                    .set(ENV_HASH_ID, hashId)
                    .where(ENV_ID.eq(envId))
                    .execute()
            }
        }
        return envId
    }

    fun update(
        dslContext: DSLContext,
        envId: Long,
        name: String,
        desc: String,
        envType: String,
        envVars: String
    ) {
        val now = LocalDateTime.now()
        with(TEnv.T_ENV) {
            dslContext.update(this)
                .set(ENV_NAME, name)
                .set(ENV_DESC, desc)
                .set(ENV_TYPE, envType)
                .set(ENV_VARS, envVars)
                .set(UPDATED_TIME, now)
                .where(ENV_ID.eq(envId))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, projectId: String): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }

    fun listEnvByProject(dslContext: DSLContext, projectId: String, limit: Int, offset: Int): List<TEnvRecord>? {
        return with(TEnv.T_ENV) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false)).limit(limit!!).offset(offset!!)
                .fetch()
        }
    }

    fun listServerEnv(dslContext: DSLContext, projectId: String): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .and(ENV_TYPE.`in`(EnvType.DEV.name, EnvType.TEST.name, EnvType.PROD.name))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }

    fun listServerEnvByIds(dslContext: DSLContext, projectId: String, envIds: Collection<Long>): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .and(ENV_ID.`in`(envIds))
                .and(ENV_TYPE.`in`(EnvType.DEV.name, EnvType.TEST.name, EnvType.PROD.name))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }

    fun listServerEnvByIdsAllType(dslContext: DSLContext, envIds: Collection<Long>): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(IS_DELETED.eq(false))
                .and(ENV_ID.`in`(envIds))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }

    fun listServerEnvByEnvNames(dslContext: DSLContext, projectId: String, envNames: List<String>): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .and(ENV_NAME.`in`(envNames))
                .and(ENV_TYPE.`in`(EnvType.DEV.name, EnvType.TEST.name, EnvType.PROD.name))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }

    fun listByType(dslContext: DSLContext, projectId: String, envType: EnvType): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(IS_DELETED.eq(false))
                .and(ENV_TYPE.eq(envType.name))
                .orderBy(ENV_ID.desc())
                .fetch()
        }
    }

    fun deleteEnv(dslContext: DSLContext, envId: Long) {
        with(TEnv.T_ENV) {
            dslContext.deleteFrom(this)
                .where(ENV_ID.eq(envId))
                .execute()
        }
    }

    fun isNameExist(dslContext: DSLContext, projectId: String, envId: Long?, envName: String): Boolean {
        with(TEnv.T_ENV) {
            return if (envId != null) {
                dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(IS_DELETED.eq(false))
                    .and(ENV_ID.ne(envId))
                    .and(ENV_NAME.eq(envName))
                    .fetchOne(0, Long::class.java)!! > 0
            } else {
                dslContext.selectCount()
                    .from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(IS_DELETED.eq(false))
                    .and(ENV_NAME.eq(envName))
                    .fetchOne(0, Long::class.java)!! > 0
            }
        }
    }

    fun listAllProjectId(dslContext: DSLContext): List<String> {
        with(TEnv.T_ENV) {
            return dslContext.selectDistinct(PROJECT_ID)
                .from(this)
                .fetch()
                .map { it.value1() }
        }
    }

    fun listPage(dslContext: DSLContext, offset: Int, limit: Int, projectId: String?): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun countByProject(dslContext: DSLContext, projectId: String?): Int {
        with(TEnv.T_ENV) {
            return dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun searchByName(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        projectId: String?,
        envName: String
    ): List<TEnvRecord> {
        with(TEnv.T_ENV) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId).and(ENV_NAME.like("%$envName%")))
                .orderBy(CREATED_TIME.desc())
                .limit(limit).offset(offset)
                .fetch()
        }
    }

    fun countByName(dslContext: DSLContext, projectId: String?, envName: String): Int {
        with(TEnv.T_ENV) {
            return dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId).and(ENV_NAME.like("%$envName%")))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getAllEnv(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<Record1<Long>>? {
        with(TEnv.T_ENV) {
            return dslContext.select(ENV_ID).from(this)
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
        with(TEnv.T_ENV) {
            dslContext.update(this)
                .set(ENV_HASH_ID, hashId)
                .where(ENV_ID.eq(id))
                .and(ENV_HASH_ID.isNull)
                .execute()
        }
    }
}
