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

package com.tencent.devops.environment.dao

import com.tencent.devops.model.environment.tables.TProjectConfig
import com.tencent.devops.model.environment.tables.records.TProjectConfigRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class ProjectConfigDao {
    fun getOrNull(dslContext: DSLContext, projectId: String): TProjectConfigRecord? {
        return with(TProjectConfig.T_PROJECT_CONFIG) {
            dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, projectId: String, userId: String): TProjectConfigRecord {
        var config = getOrNull(dslContext, projectId)
        if (config == null) {
            addDefaultConfig(dslContext, projectId, userId)
            config = getOrNull(dslContext, projectId)
        }
        return config!!
    }

    fun saveProjectConfig(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        bcsVmEnabled: Boolean,
        bcsVmQuota: Int,
        importQuota: Int,
        devCloudEnabled: Boolean,
        devCloudQuota: Int
    ) {
        with(TProjectConfig.T_PROJECT_CONFIG) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .fetchOne()
                val now = LocalDateTime.now()
                if (record == null) {
                    context.insertInto(this,
                            PROJECT_ID,
                            UPDATED_USER,
                            UPDATED_TIME,
                            BCSVM_ENALBED,
                            BCSVM_QUOTA,
                            IMPORT_QUOTA,
                            DEV_CLOUD_ENALBED,
                            DEV_CLOUD_QUOTA)
                            .values(projectId,
                                    userId,
                                    now,
                                    bcsVmEnabled,
                                    bcsVmQuota,
                                    importQuota,
                                    devCloudEnabled,
                                    devCloudQuota)
                            .execute()
                } else {
                    context.update(this)
                        .set(BCSVM_ENALBED, bcsVmEnabled)
                        .set(BCSVM_QUOTA, bcsVmQuota)
                        .set(IMPORT_QUOTA, importQuota)
                        .set(DEV_CLOUD_ENALBED, devCloudEnabled)
                        .set(DEV_CLOUD_QUOTA, devCloudQuota)
                        .set(UPDATED_USER, userId)
                        .set(UPDATED_TIME, now)
                        .where(PROJECT_ID.eq(projectId))
                        .execute()
                }
            }
        }
    }

    fun addDefaultConfig(dslContext: DSLContext, projectId: String, userId: String) {
        with(TProjectConfig.T_PROJECT_CONFIG) {
            dslContext.insertInto(this,
                    PROJECT_ID,
                    UPDATED_USER,
                    UPDATED_TIME,
                    IMPORT_QUOTA
                    )
                    .values(
                            projectId,
                            userId,
                            LocalDateTime.now(),
                            100
                    )
                    .execute()
        }
    }

    fun listProjectConfig(dslContext: DSLContext): List<TProjectConfigRecord> {
        return with(TProjectConfig.T_PROJECT_CONFIG) {
            dslContext.selectFrom(this).fetch()
        }
    }

    fun list(dslContext: DSLContext, page: Int, pageSize: Int, projectId: String?): List<TProjectConfigRecord> {
        with(TProjectConfig.T_PROJECT_CONFIG) {
            return if (projectId.isNullOrBlank()) {
                dslContext.selectFrom(this)
                        .limit(pageSize).offset((page - 1) * pageSize)
                        .fetch()
            } else {
                dslContext.selectFrom(this)
                        .where(PROJECT_ID.like("%$projectId%"))
                        .limit(pageSize).offset((page - 1) * pageSize)
                        .fetch()
            }
        }
    }

    fun count(dslContext: DSLContext, projectId: String?): Int {
        with(TProjectConfig.T_PROJECT_CONFIG) {
            return if (projectId.isNullOrBlank()) {
                dslContext.selectCount()
                        .from(TProjectConfig.T_PROJECT_CONFIG)
                        .fetchOne(0, Int::class.java)!!
            } else {
                dslContext.selectCount()
                        .from(TProjectConfig.T_PROJECT_CONFIG)
                        .where(PROJECT_ID.like("%$projectId%"))
                        .fetchOne(0, Int::class.java)!!
            }
        }
    }
}
