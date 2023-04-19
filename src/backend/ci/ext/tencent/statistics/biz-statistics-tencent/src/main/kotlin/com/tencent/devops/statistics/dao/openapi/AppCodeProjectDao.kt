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

package com.tencent.devops.statistics.dao.openapi

import com.tencent.devops.model.openapi.tables.TAppCodeProject
import com.tencent.devops.model.openapi.tables.records.TAppCodeProjectRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AppCodeProjectDao {
    fun add(
        dslContext: DSLContext,
        userName: String,
        appCode: String,
        projectId: String
    ): Boolean {
        if (exist(dslContext, appCode, projectId)) return true
        with(TAppCodeProject.T_APP_CODE_PROJECT) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(this,
                APP_CODE,
                PROJECT_ID,
                CREATOR,
                CREATE_TIME
            ).values(
                appCode,
                projectId,
                userName,
                now
            ).execute() > 0
        }
    }

    fun get(
        dslContext: DSLContext,
        appCode: String,
        projectId: String
    ): TAppCodeProjectRecord? {
        with(TAppCodeProject.T_APP_CODE_PROJECT) {
            return dslContext.selectFrom(this)
                .where(APP_CODE.eq(appCode))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne()
        }
    }

    fun list(
        dslContext: DSLContext
    ): Result<TAppCodeProjectRecord> {
        with(TAppCodeProject.T_APP_CODE_PROJECT) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun listByAppCode(
        dslContext: DSLContext,
        appCode: String
    ): Result<TAppCodeProjectRecord> {
        with(TAppCodeProject.T_APP_CODE_PROJECT) {
            return dslContext.selectFrom(this).where(APP_CODE.eq(appCode)).fetch()
        }
    }

    fun exist(
        dslContext: DSLContext,
        appCode: String,
        projectId: String
    ): Boolean {
        with(TAppCodeProject.T_APP_CODE_PROJECT) {
            return dslContext.select()
                .from(this)
                .where(APP_CODE.eq(appCode))
                .and(PROJECT_ID.eq(projectId))
                .fetch().isNotEmpty
        }
    }

    fun delete(
        dslContext: DSLContext,
        appCode: String,
        projectId: String
    ): Boolean {
        with(TAppCodeProject.T_APP_CODE_PROJECT) {
            return dslContext.delete(this)
                .where(APP_CODE.eq(appCode))
                .and(PROJECT_ID.eq(projectId)).execute() > 0
        }
    }
}
