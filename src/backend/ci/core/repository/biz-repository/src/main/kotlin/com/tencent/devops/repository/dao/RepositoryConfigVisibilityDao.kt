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

package com.tencent.devops.repository.dao

import com.tencent.devops.model.repository.tables.TRepositoryScmConfigVisibility
import com.tencent.devops.model.repository.tables.records.TRepositoryScmConfigVisibilityRecord
import com.tencent.devops.repository.pojo.RepositoryConfigVisibility
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class RepositoryConfigVisibilityDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        scmCode: String,
        deptList: List<RepositoryConfigVisibility>
    ) {
        with(TRepositoryScmConfigVisibility.T_REPOSITORY_SCM_CONFIG_VISIBILITY) {
            val now = LocalDateTime.now()
            deptList.forEach {
                dslContext.insertInto(
                    this,
                    SCM_CODE,
                    DEPT_ID,
                    DEPT_NAME,
                    CREATOR,
                    CREATE_TIME
                ).values(
                    scmCode,
                    it.deptId,
                    it.deptName,
                    userId,
                    now
                )
                .onDuplicateKeyUpdate()
                .set(CREATE_TIME, now)
                .set(CREATOR, userId)
                .set(DEPT_NAME, it.deptName)
                .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        scmCode: String,
        limit: Int,
        offset: Int
    ): Result<TRepositoryScmConfigVisibilityRecord> {
        return with(TRepositoryScmConfigVisibility.T_REPOSITORY_SCM_CONFIG_VISIBILITY) {
            dslContext.selectFrom(this)
                    .where(SCM_CODE.eq(scmCode))
                    .orderBy(CREATE_TIME.desc(), DEPT_ID)
                    .limit(limit)
                    .offset(offset)
                    .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        scmCode: String
    ): Int {
        return with(TRepositoryScmConfigVisibility.T_REPOSITORY_SCM_CONFIG_VISIBILITY) {
            dslContext.selectCount()
                    .from(this)
                    .where(SCM_CODE.eq(scmCode))
                    .fetchOne(0, Int::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        scmCode: String,
        deptList: Set<Int>
    ) {
        with(TRepositoryScmConfigVisibility.T_REPOSITORY_SCM_CONFIG_VISIBILITY) {
            dslContext.deleteFrom(this)
                    .where(SCM_CODE.eq(scmCode))
                    .and(DEPT_ID.`in`(deptList))
                    .execute()
        }
    }
}
