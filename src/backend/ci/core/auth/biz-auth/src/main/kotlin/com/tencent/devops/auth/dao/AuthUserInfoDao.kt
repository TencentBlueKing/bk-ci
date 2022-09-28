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

import com.tencent.devops.auth.entity.UserInfoEntity
import com.tencent.devops.model.auth.tables.TAuthUserInfo
import com.tencent.devops.model.auth.tables.records.TAuthUserInfoRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthUserInfoDao {

    fun createUser(dslContext: DSLContext, userInfo: UserInfoEntity): Int {
        with(TAuthUserInfo.T_AUTH_USER_INFO) {
            return dslContext.insertInto(
                this,
                USERID,
                USER_TYPE,
                EMAIL,
                PHONE,
                CREATE_TIME,
                LAST_LOGIN_TIME,
                USER_STATUS
            ).values(
                userInfo.userId,
                userInfo.userType,
                userInfo.email ?: "",
                userInfo.phone ?: "",
                LocalDateTime.now(),
                LocalDateTime.now(),
                userInfo.userStatus
            ).execute()
        }
    }

    fun updateLoginTime(dslContext: DSLContext, id: Int): Int {
        with(TAuthUserInfo.T_AUTH_USER_INFO) {
            return dslContext.update(this).set(LAST_LOGIN_TIME, LocalDateTime.now()).where(ID.eq(id)).execute()
        }
    }

    fun updateUserInfo(dslContext: DSLContext, id: Int, phone: String, email: String): Int {
        with(TAuthUserInfo.T_AUTH_USER_INFO) {
            return dslContext.update(this).set(EMAIL, email).set(PHONE, phone).where(ID.eq(id)).execute()
        }
    }

    fun getUserInfo(dslContext: DSLContext, userId: String, type: Int): TAuthUserInfoRecord? {
        with(TAuthUserInfo.T_AUTH_USER_INFO) {
            return dslContext.selectFrom(this).where(USERID.eq(userId).and(USER_TYPE.eq(type))).fetchAny()
        }
    }

    fun listUserInfo(dslContext: DSLContext, userType: Int): Result<TAuthUserInfoRecord> {
        with(TAuthUserInfo.T_AUTH_USER_INFO) {
            return dslContext.selectFrom(this).where(USER_TYPE.eq(userType)).fetch()
        }
    }
}
