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

import com.tencent.devops.model.ai.tables.TAiUserLlmConfig
import com.tencent.devops.model.ai.tables.records.TAiUserLlmConfigRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/** 用户自定义大模型配置 DAO，对应 T_AI_USER_LLM_CONFIG 表。 */
@Repository
class UserLlmConfigDao {

    fun getByUserId(
        dslContext: DSLContext,
        userId: String
    ): TAiUserLlmConfigRecord? {
        with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .fetchOne()
        }
    }

    fun upsert(
        dslContext: DSLContext,
        record: TAiUserLlmConfigRecord
    ): Int {
        with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            return dslContext.insertInto(
                this,
                USER_ID,
                BASE_URL,
                MODEL_NAME,
                API_KEY,
                BK_APP_CODE,
                BK_APP_SECRET,
                ENABLED,
                CONNECT_TIMEOUT_SECONDS,
                READ_TIMEOUT_SECONDS,
                WRITE_TIMEOUT_SECONDS,
                EXECUTION_TIMEOUT_SECONDS,
                MAX_ATTEMPTS,
                INITIAL_BACKOFF_SECONDS,
                MAX_BACKOFF_SECONDS,
                BACKOFF_MULTIPLIER,
                CREATED_TIME,
                UPDATED_TIME
            ).values(
                record.userId,
                record.baseUrl,
                record.modelName,
                record.apiKey,
                record.bkAppCode,
                record.bkAppSecret,
                record.enabled,
                record.connectTimeoutSeconds,
                record.readTimeoutSeconds,
                record.writeTimeoutSeconds,
                record.executionTimeoutSeconds,
                record.maxAttempts,
                record.initialBackoffSeconds,
                record.maxBackoffSeconds,
                record.backoffMultiplier,
                record.createdTime,
                record.updatedTime
            ).onDuplicateKeyUpdate()
                .set(BASE_URL, record.baseUrl)
                .set(MODEL_NAME, record.modelName)
                .set(API_KEY, record.apiKey)
                .set(BK_APP_CODE, record.bkAppCode)
                .set(BK_APP_SECRET, record.bkAppSecret)
                .set(ENABLED, record.enabled)
                .set(CONNECT_TIMEOUT_SECONDS, record.connectTimeoutSeconds)
                .set(READ_TIMEOUT_SECONDS, record.readTimeoutSeconds)
                .set(WRITE_TIMEOUT_SECONDS, record.writeTimeoutSeconds)
                .set(EXECUTION_TIMEOUT_SECONDS, record.executionTimeoutSeconds)
                .set(MAX_ATTEMPTS, record.maxAttempts)
                .set(INITIAL_BACKOFF_SECONDS, record.initialBackoffSeconds)
                .set(MAX_BACKOFF_SECONDS, record.maxBackoffSeconds)
                .set(BACKOFF_MULTIPLIER, record.backoffMultiplier)
                .set(UPDATED_TIME, record.updatedTime)
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        userId: String
    ): Int {
        with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(USER_ID.eq(userId))
                .execute()
        }
    }
}
