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
 *
 */

package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.tables.TAuthMigration
import com.tencent.devops.model.auth.tables.records.TAuthMigrationRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthMigrationDao {

    fun create(
        dslContext: DSLContext,
        projectCode: String,
        status: Int,
        routerTag: String
    ) {
        val now = LocalDateTime.now()
        with(TAuthMigration.T_AUTH_MIGRATION) {
            dslContext.insertInto(
                this,
                PROJECT_CODE,
                STATUS,
                ROUTER_TAG,
                START_TIME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectCode,
                status,
                routerTag,
                now,
                now,
                now
            ).onDuplicateKeyUpdate()
                .set(STATUS, status)
                .set(ERROR_MESSAGE, "")
                .set(START_TIME, now)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    fun updateGroupCount(
        dslContext: DSLContext,
        projectCode: String,
        beforeGroupCount: Int,
        afterGroupCount: Int
    ) {
        with(TAuthMigration.T_AUTH_MIGRATION) {
            dslContext.update(this)
                .set(BEFORE_GROUP_COUNT, beforeGroupCount)
                .set(AFTER_GROUP_COUNT, afterGroupCount)
                .where(PROJECT_CODE.eq(projectCode))
                .execute()
        }
    }

    fun updateResourceCount(
        dslContext: DSLContext,
        projectCode: String,
        resourceCountInfo: String
    ) {
        with(TAuthMigration.T_AUTH_MIGRATION) {
            dslContext.update(this)
                .set(RESOURCE_COUNT, resourceCountInfo)
                .where(PROJECT_CODE.eq(projectCode))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectCode: String,
        status: Int,
        errorMessage: String? = null,
        totalTime: Long?
    ) {
        with(TAuthMigration.T_AUTH_MIGRATION) {
            val update = dslContext.update(this)
                .set(STATUS, status)
                .set(END_TIME, LocalDateTime.now())
            if (totalTime != null) {
                update.set(TOTAL_TIME, totalTime)
            }
            update.set(ERROR_MESSAGE, errorMessage)
            update.where(PROJECT_CODE.eq(projectCode)).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectCode: String
    ): TAuthMigrationRecord? {
        with(TAuthMigration.T_AUTH_MIGRATION) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .fetchOne()
        }
    }
}
