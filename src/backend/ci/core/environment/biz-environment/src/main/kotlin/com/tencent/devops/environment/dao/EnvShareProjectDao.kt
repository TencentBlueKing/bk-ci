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

import com.tencent.devops.environment.pojo.AddSharedProjectInfo
import com.tencent.devops.model.environment.tables.TEnvShareProject
import com.tencent.devops.model.environment.tables.records.TEnvShareProjectRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EnvShareProjectDao {
    fun listPage(
        dslContext: DSLContext,
        projectId: String,
        envId: Long,
        name: String?,
        offset: Int,
        limit: Int
    ): List<TEnvShareProjectRecord> {
        with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
            val where = dslContext.selectFrom(this)
                .where(MAIN_PROJECT_ID.eq(projectId))
                .and((ENV_ID.eq(envId)))
            if (!name.isNullOrBlank()) {
                where.and(SHARED_PROJECT_NAME.like("%$name%"))
            }
            return where.orderBy(UPDATE_TIME.desc()).limit(limit).offset(offset)
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String,
        envId: Long,
        name: String?
    ): Int {
        with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
            val where = dslContext.selectCount().from(this)
                .where(MAIN_PROJECT_ID.eq(projectId))
                .and((ENV_ID.eq(envId)))
            if (!name.isNullOrBlank()) {
                where.and(SHARED_PROJECT_NAME.like("%$name%"))
            }
            return where.fetchOne(0, Int::class.java)!!
        }
    }

    fun get(
        dslContext: DSLContext,
        envName: String,
        sharedProjectId: String,
        mainProjectId: String
    ): List<TEnvShareProjectRecord> {
        with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
            return dslContext.selectFrom(this)
                .where(ENV_NAME.eq(envName))
                .and(MAIN_PROJECT_ID.eq(mainProjectId))
                .and(SHARED_PROJECT_ID.eq(sharedProjectId))
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        envName: String,
        mainProjectId: String
    ): List<TEnvShareProjectRecord> {
        with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
            return dslContext.selectFrom(this)
                .where(ENV_NAME.eq(envName))
                .and(MAIN_PROJECT_ID.eq(mainProjectId))
                .fetch()
        }
    }

    @SuppressWarnings("LongParameterList")
    fun batchSave(
        dslContext: DSLContext,
        userId: String,
        envId: Long,
        envName: String,
        mainProjectId: String,
        sharedProjects: List<AddSharedProjectInfo>
    ) {
        if (sharedProjects.isEmpty()) {
            return
        }
        val now = LocalDateTime.now()
        dslContext.batch(sharedProjects.map {
            with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
                dslContext.insertInto(
                    this,
                    ENV_ID,
                    ENV_NAME,
                    MAIN_PROJECT_ID,
                    SHARED_PROJECT_ID,
                    SHARED_PROJECT_NAME,
                    TYPE,
                    CREATOR,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    envId,
                    envName,
                    mainProjectId,
                    it.getFinalProjectId(),
                    it.name,
                    it.type.name,
                    userId,
                    now,
                    now
                ).onDuplicateKeyUpdate()
                    .set(ENV_NAME, envName)
                    .set(SHARED_PROJECT_NAME, it.getFinalProjectId())
                    .set(TYPE, it.type.name)
                    .set(CREATOR, userId)
                    .set(UPDATE_TIME, now)
            }
        }).execute()
    }

    fun deleteByEnvAndMainProj(
        dslContext: DSLContext,
        envId: Long,
        mainProjectId: String
    ) {
        with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
            dslContext.deleteFrom(this)
                .where(ENV_ID.eq(envId))
                .and(MAIN_PROJECT_ID.eq(mainProjectId))
                .execute()
        }
    }

    fun deleteBySharedProj(
        dslContext: DSLContext,
        envId: Long,
        mainProjectId: String,
        sharedProjectId: String
    ) {
        with(TEnvShareProject.T_ENV_SHARE_PROJECT) {
            dslContext.deleteFrom(this)
                .where(ENV_ID.eq(envId))
                .and(MAIN_PROJECT_ID.eq(mainProjectId))
                .and(SHARED_PROJECT_ID.eq(sharedProjectId))
                .execute()
        }
    }
}
