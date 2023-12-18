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

import com.tencent.devops.auth.pojo.AuthProjectUserCountDaily
import com.tencent.devops.model.auth.tables.TAuthUserDaily
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AuthUserDailyDao {


    fun save(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ) {
        with(TAuthUserDaily.T_AUTH_USER_DAILY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                THE_DATE
            ).values(
                projectId,
                userId,
                LocalDate.now()
            ).onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun countProjectUserCountDaily(
        dslContext: DSLContext,
        theDate: LocalDate,
        projectId: String? = null
    ): Long {
        return with(TAuthUserDaily.T_AUTH_USER_DAILY) {
            dslContext.select(DSL.countDistinct(PROJECT_ID)).from(this)
                .where(THE_DATE.eq(theDate))
                .let {
                    if (projectId.isNullOrEmpty()) {
                        it
                    } else {
                        it.and(PROJECT_ID.eq(projectId))
                    }
                }.fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun listProjectUserCountDaily(
        dslContext: DSLContext,
        theDate: LocalDate,
        projectId: String? = null,
        offset: Int,
        limit: Int
    ): List<AuthProjectUserCountDaily> {
        with(TAuthUserDaily.T_AUTH_USER_DAILY) {
            return dslContext.select(PROJECT_ID, DSL.count().`as`("userCount")).from(this)
                .where(THE_DATE.eq(theDate))
                .let {
                    if (projectId.isNullOrEmpty()) {
                        it
                    } else {
                        it.and(PROJECT_ID.eq(projectId))
                    }
                }
                .groupBy(PROJECT_ID)
                .orderBy(PROJECT_ID.asc())
                .offset(offset)
                .limit(limit)
                .fetch().map {
                    AuthProjectUserCountDaily(
                        projectId = it.value1(),
                        userCount = it.value2()
                    )
                }
        }
    }
}
