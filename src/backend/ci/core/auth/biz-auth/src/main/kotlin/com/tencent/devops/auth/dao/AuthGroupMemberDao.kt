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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.entity.GroupMemberInfo
import com.tencent.devops.model.auth.tables.TAuthGroupMember
import com.tencent.devops.model.auth.tables.records.TAuthGroupMemberRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthGroupMemberDao {

    fun createGroupMember(
        dslContext: DSLContext,
        groupMember: GroupMemberInfo
    ): Int {
        with(TAuthGroupMember.T_AUTH_GROUP_MEMBER) {
            return dslContext.insertInto(
                this,
                GROUP_ID,
                USER_ID,
                USER_TYPE,
                PROJECT_ID,
                EXPIRED_TIEM,
                EXPIRED_TYPE,
                CREATE_TIME,
            ).values(
                groupMember.groupId,
                groupMember.userId,
                groupMember.userType,
                groupMember.projectCode,
                LocalDateTime.now().plusDays(groupMember.expiredDay),
                0,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun updateExpiredStatus(
        dslContext: DSLContext,
        id: Int,
        expiredStatus: Int
    ): Int {
        with(TAuthGroupMember.T_AUTH_GROUP_MEMBER) {
            return dslContext.update(this).set(EXPIRED_TYPE, expiredStatus)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateGroupMemberExpired(
        dslContext: DSLContext,
        userId: String,
        groupId: Int,
        expiredDay: Long
    ): Int {
        with(TAuthGroupMember.T_AUTH_GROUP_MEMBER) {
            return dslContext.update(this).set(EXPIRED_TIEM, LocalDateTime.now().plusDays(expiredDay))
                .where(USER_ID.eq(userId).and(GROUP_ID.eq(groupId)))
                .execute()
        }
    }

    fun deleteGroupMember(
        dslContext: DSLContext,
        groupId: Int,
        userId: String
    ): Int {
        with(TAuthGroupMember.T_AUTH_GROUP_MEMBER) {
            return dslContext.delete(this)
                .where(USER_ID.eq(userId).and(GROUP_ID.eq(groupId)))
                .execute()
        }
    }

    fun getGroupMemberByGroup(
        dslContext: DSLContext,
        groupId: Int
    ): TAuthGroupMemberRecord? {
        with(TAuthGroupMember.T_AUTH_GROUP_MEMBER) {
            return dslContext.selectFrom(this).where(GROUP_ID.eq(groupId)).fetchAny()
        }
    }

    fun getGroupMemberByProject(
        dslContext: DSLContext,
        projectCode: String,
        userId: String?
    ): Result<TAuthGroupMemberRecord> {
        with(TAuthGroupMember.T_AUTH_GROUP_MEMBER) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectCode))
            if (!userId.isNullOrEmpty()) {
                conditions.add(USER_ID.eq(userId))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }
}