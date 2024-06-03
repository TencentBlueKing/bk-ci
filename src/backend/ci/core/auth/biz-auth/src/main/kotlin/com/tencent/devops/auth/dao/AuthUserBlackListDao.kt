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

import com.tencent.devops.model.auth.tables.TAuthUserBlacklist
import com.tencent.devops.model.auth.tables.records.TAuthUserBlacklistRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AuthUserBlackListDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        remark: String
    ): Int {
        with(TAuthUserBlacklist.T_AUTH_USER_BLACKLIST) {
            return dslContext.insertInto(
                this,
                USER_ID,
                REMARK,
                STATUS,
                CREATE_TIME
            ).values(
                userId,
                remark,
                true,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        userId: String
    ): TAuthUserBlacklistRecord? {
        with(TAuthUserBlacklist.T_AUTH_USER_BLACKLIST) {
            return dslContext.selectFrom(this).where(USER_ID.eq(userId).and(STATUS.eq(true))).fetchAny()
        }
    }

    fun list(
        dslContext: DSLContext
    ): Result<TAuthUserBlacklistRecord>? {
        with(TAuthUserBlacklist.T_AUTH_USER_BLACKLIST) {
            return dslContext.selectFrom(this).where(STATUS.eq(true)).fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        userId: String
    ): Int {
        with(TAuthUserBlacklist.T_AUTH_USER_BLACKLIST) {
            return dslContext.update(this).set(STATUS, false).where(USER_ID.eq(userId).and(STATUS.eq(true))).execute()
        }
    }
}
