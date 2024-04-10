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

import com.tencent.devops.model.openapi.tables.TAppCodeGroup
import com.tencent.devops.model.openapi.tables.records.TAppCodeGroupRecord
import com.tencent.devops.statistics.pojo.openapi.AppCodeGroup
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AppCodeGroupDao {
    fun set(
        dslContext: DSLContext,
        userName: String,
        appCode: String,
        appCodeGroup: AppCodeGroup
    ): Boolean {
        val exist = exist(dslContext, appCode)
        val now = LocalDateTime.now()
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return if (exist) {
                dslContext.update(this)
                    .set(BG_ID, appCodeGroup.bgId)
                    .set(BG_NAME, appCodeGroup.bgName)
                    .set(DEPT_ID, appCodeGroup.deptId)
                    .set(DEPT_NAME, appCodeGroup.deptName)
                    .set(CENTER_ID, appCodeGroup.centerId)
                    .set(CENTER_NAME, appCodeGroup.centerName)
                    .set(UPDATER, userName)
                    .set(UPDATE_TIME, now)
                    .where(APP_CODE.eq(appCode))
                    .execute() > 0
            } else {
                dslContext.insertInto(this,
                    APP_CODE,
                    BG_ID,
                    BG_NAME,
                    DEPT_ID,
                    DEPT_NAME,
                    CENTER_ID,
                    CENTER_NAME,
                    CREATOR,
                    CREATE_TIME,
                    UPDATER,
                    UPDATE_TIME
                ).values(
                    appCode,
                    appCodeGroup.bgId,
                    appCodeGroup.bgName,
                    appCodeGroup.deptId,
                    appCodeGroup.deptName,
                    appCodeGroup.centerId,
                    appCodeGroup.centerName,
                    userName,
                    now,
                    userName,
                    now
                ).execute() > 0
            }
        }
    }

    fun get(
        dslContext: DSLContext,
        appCode: String
    ): TAppCodeGroupRecord? {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.selectFrom(this)
                .where(APP_CODE.eq(appCode))
                .fetchOne()
        }
    }

    fun list(
        dslContext: DSLContext
    ): Result<TAppCodeGroupRecord> {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun exist(
        dslContext: DSLContext,
        appCode: String
    ): Boolean {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.select()
                .from(this)
                .where(APP_CODE.eq(appCode)).fetch().isNotEmpty
        }
    }

    fun delete(
        dslContext: DSLContext,
        appCode: String
    ): Boolean {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.delete(this)
                .where(APP_CODE.eq(appCode)).execute() > 0
        }
    }
}
