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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TProjectPipelineCallback
import com.tencent.devops.model.process.tables.records.TProjectPipelineCallbackRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class ProjectPipelineCallbackDao {

    /**
     * 可直接更新或插入
     */
    fun save(
        dslContext: DSLContext,
        projectId: String,
        events: String,
        userId: String,
        callbackUrl: String,
        secretToken: String?,
        id: Long? = null
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                EVENTS,
                CREATED_TIME,
                UPDATED_TIME,
                CREATOR,
                UPDATOR,
                CALLBACK_URL,
                SECRET_TOKEN,
                ID
            ).values(
                projectId,
                events,
                now,
                now,
                userId,
                userId,
                callbackUrl,
                secretToken,
                id
            ).onDuplicateKeyUpdate()
                .set(UPDATED_TIME, now)
                .set(UPDATOR, userId)
                .set(CALLBACK_URL, callbackUrl)
                .set(SECRET_TOKEN, secretToken).execute()
        }
    }

    fun listProjectCallback(
        dslContext: DSLContext,
        projectId: String,
        events: String,
        enable: Boolean = true
    ): Result<TProjectPipelineCallbackRecord> {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(EVENTS.eq(events))
                .and(ENABLE.eq(enable))
                .fetch()
        }
    }

    fun listByPage(
        dslContext: DSLContext,
        projectId: String,
        offset: Int,
        limit: Int
    ): Result<TProjectPipelineCallbackRecord> {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun countByPage(
        dslContext: DSLContext,
        projectId: String
    ): Long {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ): TProjectPipelineCallbackRecord? {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id).and(PROJECT_ID.eq(projectId)))
                .fetchOne()
        }
    }

    fun deleteById(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.deleteFrom(this).where(ID.eq(id).and(PROJECT_ID.eq(projectId))).execute()
        }
    }

    fun deleteByProjectId(
        dslContext: DSLContext,
        projectId: String
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).execute()
        }
    }

    fun disable(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.update(this)
                .set(ENABLE, false)
                .where(ID.eq(id).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun enable(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.update(this)
                .set(ENABLE, true)
                .where(ID.eq(id).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun getDisableCallbackList(
        dslContext: DSLContext,
        projectId: String?,
        url: String?,
        offset: Int,
        limit: Int
    ): Result<TProjectPipelineCallbackRecord> {
        return with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            val conditions = mutableListOf(
                ENABLE.eq(false)
            )
            if (!projectId.isNullOrEmpty()) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (!url.isNullOrEmpty()) {
                conditions.add(CALLBACK_URL.eq(url))
            }
            dslContext.selectFrom(this)
                .where(conditions)
                .limit(offset, limit)
                .fetch()
        }
    }

    fun enableByIds(
        dslContext: DSLContext,
        projectId: String,
        ids: List<Int>
    ) {
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            val conditions = mutableListOf(
                ENABLE.eq(false),
                ID.`in`(ids)
            )
            dslContext.update(this)
                .set(ENABLE, true)
                .where(conditions)
                .execute()
        }
    }
}
