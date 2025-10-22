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
package com.tencent.devops.gpt.dao

import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.gpt.service.config.GptGatewayCondition
import com.tencent.devops.model.plugin.tables.TAiScore
import com.tencent.devops.model.plugin.tables.records.TAiScoreRecord
import org.jooq.DSLContext
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Repository

@Repository
@Conditional(GptGatewayCondition::class)
class AIScoreDao {

    fun create(
        dslContext: DSLContext,
        label: String,
        aiMsg: String,
        systemMsg: String,
        userMsg: String,
        goodUserIds: Set<String>,
        badUserIds: Set<String>
    ) {
        with(TAiScore.T_AI_SCORE) {
            dslContext.insertInto(
                this,
                LABEL,
                GOOD_USERS,
                BAD_USERS,
                AI_MSG,
                SYSTEM_MSG,
                USER_MSG
            ).values(
                label,
                goodUserIds.joinToString(","),
                badUserIds.joinToString(","),
                aiMsg,
                systemMsg,
                userMsg
            ).onDuplicateKeyUpdate()
                .set(GOOD_USERS, goodUserIds.joinToString(","))
                .set(BAD_USERS, badUserIds.joinToString(","))
                .execute()
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        label: String
    ): TAiScoreRecord? {
        with(TAiScore.T_AI_SCORE) {
            return dslContext.selectFrom(this)
                .where(LABEL.eq(label))
                .and(ARCHIVE.eq(ByteUtils.bool2Byte(false)))
                .orderBy(CREATE_TIME.desc())
                .fetchAny()
        }
    }

    fun updateUsers(
        dslContext: DSLContext,
        id: Long,
        goodUserIds: Set<String>,
        badUserIds: Set<String>
    ) {
        with(TAiScore.T_AI_SCORE) {
            dslContext.update(this)
                .set(GOOD_USERS, goodUserIds.joinToString(","))
                .set(BAD_USERS, badUserIds.joinToString(","))
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateMsg(
        dslContext: DSLContext,
        id: Long,
        aiMsg: String,
        systemMsg: String,
        userMsg: String
    ) {
        with(TAiScore.T_AI_SCORE) {
            dslContext.update(this)
                .set(AI_MSG, aiMsg)
                .set(SYSTEM_MSG, systemMsg)
                .set(USER_MSG, userMsg)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun archive(dslContext: DSLContext, label: String) {
        with(TAiScore.T_AI_SCORE) {
            dslContext.update(this)
                .set(ARCHIVE, ByteUtils.bool2Byte(true))
                .where(LABEL.eq(label)).execute()
        }
    }
}
