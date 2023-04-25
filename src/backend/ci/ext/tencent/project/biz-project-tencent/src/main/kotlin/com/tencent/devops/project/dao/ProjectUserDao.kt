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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TUser
import com.tencent.devops.model.project.tables.records.TUserRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectUserDao {
    fun list(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<TUserRecord>? {
        return with(TUser.T_USER) {
            dslContext.selectFrom(this).where(USER_TYPE.eq(false)).limit(offset, limit).fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        userId: String
    ) {
        return with(TUser.T_USER) {
            dslContext.delete(this).where(USER_ID.eq(userId)).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        userId: String,
        bgId: Int,
        bgName: String,
        deptId: Int,
        deptName: String,
        centerId: Int,
        centerName: String,
        groupId: Int,
        groupName: String
    ) {
        with(TUser.T_USER) {
            dslContext.update(this)
                .set(BG_ID, bgId)
                .set(BG_NAME, bgName)
                .set(DEPT_ID, deptId)
                .set(DEPT_NAME, deptName)
                .set(CENTER_ID, centerId)
                .set(CENTER_NAME, centerName)
                .set(GROYP_ID, groupId)
                .set(GROUP_NAME, groupName)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(USER_ID.eq(userId))
                .execute()
        }
    }
}
