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

package com.tencent.devops.auth.dao

import com.tencent.devops.model.auth.Tables
import com.tencent.devops.model.auth.tables.records.TAuthManagerWhitelistRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ManagerWhiteDao {

    fun create(
        dslContext: DSLContext,
        managerId: Int,
        userId: String
    ) {
        with(Tables.T_AUTH_MANAGER_WHITELIST) {
            dslContext.insertInto(this,
                MANAGER_ID,
                USER_ID).values(
                managerId,
                userId
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Int
    ): Int {
        with(Tables.T_AUTH_MANAGER_WHITELIST) {
            return dslContext.delete(this).where(ID.eq(id)).execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        managerId: Int
    ): Result<TAuthManagerWhitelistRecord>? {
        with(Tables.T_AUTH_MANAGER_WHITELIST) {
            return dslContext.selectFrom(this).where(MANAGER_ID.eq(managerId)).fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        managerId: Int,
        userId: String
    ): TAuthManagerWhitelistRecord? {
        with(Tables.T_AUTH_MANAGER_WHITELIST) {
            return dslContext.selectFrom(this).where(MANAGER_ID.eq(managerId).and(USER_ID.eq(userId))).fetchAny()
        }
    }
}
