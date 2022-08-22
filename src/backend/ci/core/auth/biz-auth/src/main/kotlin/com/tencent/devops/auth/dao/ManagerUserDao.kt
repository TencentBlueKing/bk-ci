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

import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.model.auth.tables.TAuthManagerUser
import com.tencent.devops.model.auth.tables.records.TAuthManagerUserRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL.currentLocalDateTime
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class ManagerUserDao {

    fun create(dslContext: DSLContext, managerUserInfo: ManagerUserEntity): Int {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.insertInto(
                this,
                USER_ID,
                MANAGER_ID,
                END_TIME,
                CREATE_USER,
                UPDATE_USER
            ).values(
                managerUserInfo.userId,
                managerUserInfo.managerId,
                Timestamp(managerUserInfo.timeoutTime).toLocalDateTime(),
                managerUserInfo.createUser,
                ""
            ).execute()
        }
    }

    fun delete(dslContext: DSLContext, managerId: Int, userId: String): Int {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.deleteFrom(this).where(MANAGER_ID.eq(managerId).and(USER_ID.eq(userId))).execute()
        }
    }

    fun list(dslContext: DSLContext, managerId: Int): Result<TAuthManagerUserRecord>? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(
                MANAGER_ID.eq(managerId)
                    .and(END_TIME.gt(LocalDateTime.now()))
            ).orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun get(dslContext: DSLContext, managerId: Int, userId: String): TAuthManagerUserRecord? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(MANAGER_ID.eq(managerId).and(USER_ID.eq(userId))).fetchOne()
        }
    }

    fun getExpiringRecord(dslContext: DSLContext, managerId: Int, userId: String): TAuthManagerUserRecord? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(
                MANAGER_ID.eq(managerId)
                    .and(USER_ID.eq(userId))
                    .and(END_TIME.sub(EXPIRE_TIME).le(currentLocalDateTime()))
            ).fetchOne()
        }
    }

    fun listExpiringRecords(dslContext: DSLContext): Result<TAuthManagerUserRecord>? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(
                END_TIME.sub(EXPIRE_TIME).le(currentLocalDateTime())
            ).fetch()
        }
    }

    fun updateRecordsExpireTime(dslContext: DSLContext, managerId: Int, userId: String): Int {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.update(this).set(END_TIME, END_TIME.add(HALF_A_YEAR))
                .where(MANAGER_ID.eq(managerId).and(USER_ID.eq(userId))).execute()
        }
    }

    fun getByUser(dslContext: DSLContext, userId: String): Result<TAuthManagerUserRecord>? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where((USER_ID.eq(userId))).fetch()
        }
    }

    fun count(dslContext: DSLContext, managerId: Int): Int {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectCount().from(this).where(
                MANAGER_ID.eq(managerId)
                    .and(END_TIME.gt(LocalDateTime.now()))
            ).fetchOne(0, Int::class.java)!!
        }
    }

    fun allCount(dslContext: DSLContext): Int {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectCount().from(this).fetchOne(0, Int::class.java)!!
        }
    }

    fun timeoutList(dslContext: DSLContext, limit: Int, offset: Int): Result<TAuthManagerUserRecord>? {
        with(TAuthManagerUser.T_AUTH_MANAGER_USER) {
            return dslContext.selectFrom(this).where(END_TIME.le(LocalDateTime.now()))
                .limit(limit).offset(offset).fetch()
        }
    }

    companion object {
        const val HALF_A_YEAR = 182
        const val EXPIRE_TIME = 7
    }
}
