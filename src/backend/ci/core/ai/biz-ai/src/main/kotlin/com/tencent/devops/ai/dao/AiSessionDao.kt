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

package com.tencent.devops.ai.dao

import com.tencent.devops.model.ai.tables.TAiSession
import com.tencent.devops.model.ai.tables.records.TAiSessionRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/** AI 会话 DAO，对应 T_AI_SESSION 表。 */
@Repository
class AiSessionDao {

    fun create(
        dslContext: DSLContext,
        id: String,
        userId: String,
        projectId: String?,
        title: String
    ) {
        val now = LocalDateTime.now()
        with(TAiSession.T_AI_SESSION) {
            dslContext.insertInto(
                this,
                ID, USER_ID, PROJECT_ID, TITLE, CREATED_TIME, UPDATED_TIME
            ).values(
                id, userId, projectId, title, now, now
            ).execute()
        }
    }

    fun getById(dslContext: DSLContext, sessionId: String): TAiSessionRecord? {
        with(TAiSession.T_AI_SESSION) {
            return dslContext.selectFrom(this)
                .where(ID.eq(sessionId))
                .fetchOne()
        }
    }

    fun listByUserAndProject(
        dslContext: DSLContext,
        userId: String,
        projectId: String?
    ): Result<TAiSessionRecord> {
        with(TAiSession.T_AI_SESSION) {
            val query = dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
            if (projectId != null) {
                query.and(PROJECT_ID.eq(projectId))
            } else {
                query.and(PROJECT_ID.isNull)
            }
            return query.orderBy(UPDATED_TIME.desc()).fetch()
        }
    }

    fun getLatest(
        dslContext: DSLContext,
        userId: String,
        projectId: String?
    ): TAiSessionRecord? {
        with(TAiSession.T_AI_SESSION) {
            val query = dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
            if (projectId != null) {
                query.and(PROJECT_ID.eq(projectId))
            } else {
                query.and(PROJECT_ID.isNull)
            }
            return query.orderBy(UPDATED_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun updateTitle(
        dslContext: DSLContext,
        sessionId: String,
        title: String
    ): Int {
        with(TAiSession.T_AI_SESSION) {
            return dslContext.update(this)
                .set(TITLE, title)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(sessionId))
                .execute()
        }
    }

    fun updateTime(dslContext: DSLContext, sessionId: String) {
        with(TAiSession.T_AI_SESSION) {
            dslContext.update(this)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(sessionId))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, sessionId: String): Int {
        with(TAiSession.T_AI_SESSION) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(sessionId))
                .execute()
        }
    }
}
