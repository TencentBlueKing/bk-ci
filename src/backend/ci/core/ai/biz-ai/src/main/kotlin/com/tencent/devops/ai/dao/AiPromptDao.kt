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

import com.tencent.devops.model.ai.tables.TAiPrompt
import com.tencent.devops.model.ai.tables.records.TAiPromptRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/** 用户提示词 DAO，对应 T_AI_PROMPT 表。 */
@Repository
class AiPromptDao {

    fun create(
        dslContext: DSLContext,
        id: String,
        userId: String,
        title: String,
        content: String,
        interactionType: String
    ) {
        val now = LocalDateTime.now()
        with(TAiPrompt.T_AI_PROMPT) {
            dslContext.insertInto(
                this,
                ID,
                USER_ID,
                TITLE,
                CONTENT,
                INTERACTION_TYPE,
                CREATED_TIME,
                UPDATED_TIME
            ).values(
                id,
                userId,
                title,
                content,
                interactionType,
                now,
                now
            ).execute()
        }
    }

    fun getById(dslContext: DSLContext, promptId: String): TAiPromptRecord? {
        with(TAiPrompt.T_AI_PROMPT) {
            return dslContext.selectFrom(this)
                .where(ID.eq(promptId))
                .fetchOne()
        }
    }

    fun listByUserId(
        dslContext: DSLContext,
        userId: String
    ): Result<TAiPromptRecord> {
        with(TAiPrompt.T_AI_PROMPT) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .orderBy(UPDATED_TIME.desc())
                .fetch()
        }
    }

    fun update(
        dslContext: DSLContext,
        promptId: String,
        title: String,
        content: String,
        interactionType: String
    ): Int {
        with(TAiPrompt.T_AI_PROMPT) {
            return dslContext.update(this)
                .set(TITLE, title)
                .set(CONTENT, content)
                .set(INTERACTION_TYPE, interactionType)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(promptId))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, promptId: String): Int {
        with(TAiPrompt.T_AI_PROMPT) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(promptId))
                .execute()
        }
    }
}
