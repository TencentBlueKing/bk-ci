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
import com.tencent.devops.project.pojo.user.UserDeptDetail
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

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
        userDeptDetail: UserDeptDetail
    ) {
        with(TUser.T_USER) {
            val baseStep = dslContext.update(this)
                .set(BG_ID, userDeptDetail.bgId.toInt())
                .set(BG_NAME, userDeptDetail.bgName)
                .set(DEPT_ID, userDeptDetail.deptId.toInt())
                .set(DEPT_NAME, userDeptDetail.deptName)
                .set(CENTER_ID, userDeptDetail.centerId.toInt())
                .set(CENTER_NAME, userDeptDetail.centerName)
                .set(GROYP_ID, userDeptDetail.groupId.toInt())
                .set(GROUP_NAME, userDeptDetail.groupName)
                .set(UPDATE_TIME, LocalDateTime.now())
            userDeptDetail.businessLineId?.let {
                baseStep.set(BUSINESS_LINE_ID, it.toLong())
            }
            userDeptDetail.businessLineName?.let {
                baseStep.set(BUSINESS_LINE_NAME, it)
            }
            baseStep.where(USER_ID.eq(userDeptDetail.userId!!)).execute()
        }
    }
}
