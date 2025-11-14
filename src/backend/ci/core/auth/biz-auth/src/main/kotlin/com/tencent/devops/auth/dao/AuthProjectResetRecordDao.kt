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
 *
 */

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.enum.ProjectResetStatus
import com.tencent.devops.auth.pojo.enum.ProjectResetType
import com.tencent.devops.model.auth.tables.TAuthProjectResetRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthProjectResetRecordDao {

    /**
     * 创建项目重置记录
     */
    fun create(
        dslContext: DSLContext,
        taskId: String,
        projectCode: String,
        resetType: ProjectResetType,
        migrateResource: Boolean,
        filterResourceTypes: String?,
        filterActions: String?,
        operator: String? = null
    ): Long {
        val now = LocalDateTime.now()
        with(TAuthProjectResetRecord.T_AUTH_PROJECT_RESET_RECORD) {
            val record = dslContext.insertInto(
                this,
                TASK_ID,
                PROJECT_CODE,
                RESET_TYPE,
                MIGRATE_RESOURCE,
                FILTER_RESOURCE_TYPES,
                FILTER_ACTIONS,
                STATUS,
                START_TIME,
                OPERATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                taskId,
                projectCode,
                resetType.name,
                if (migrateResource) 1 else 0,
                filterResourceTypes,
                filterActions,
                ProjectResetStatus.PROCESSING.name,
                now,
                operator,
                now,
                now
            ).returning(ID)
                .fetchOne()
            return record?.id ?: 0L
        }
    }

    /**
     * 更新重置记录状态
     */
    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: ProjectResetStatus,
        errorMessage: String? = null
    ) {
        val now = LocalDateTime.now()
        with(TAuthProjectResetRecord.T_AUTH_PROJECT_RESET_RECORD) {
            val update = dslContext.update(this)
                .set(STATUS, status.name)
                .set(END_TIME, now)
                .set(UPDATE_TIME, now)

            if (errorMessage != null) {
                update.set(ERROR_MESSAGE, errorMessage)
            }

            // 计算总耗时
            val record = dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()

            if (record != null) {
                val startTime = record.startTime
                val totalTime = java.time.Duration.between(startTime, now).toMillis()
                update.set(TOTAL_TIME, totalTime)
            }

            update.where(ID.eq(id)).execute()
        }
    }

    /**
     * 根据任务ID查询重置记录
     */
    fun listByTaskId(
        dslContext: DSLContext,
        taskId: String
    ): List<com.tencent.devops.model.auth.tables.records.TAuthProjectResetRecordRecord> {
        with(TAuthProjectResetRecord.T_AUTH_PROJECT_RESET_RECORD) {
            return dslContext.selectFrom(this)
                .where(TASK_ID.eq(taskId))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    /**
     * 根据项目代码查询重置记录
     */
    fun listByProjectCode(
        dslContext: DSLContext,
        projectCode: String,
        limit: Int = 100
    ): List<com.tencent.devops.model.auth.tables.records.TAuthProjectResetRecordRecord> {
        with(TAuthProjectResetRecord.T_AUTH_PROJECT_RESET_RECORD) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .orderBy(CREATE_TIME.desc())
                .limit(limit)
                .fetch()
        }
    }

    /**
     * 根据ID查询重置记录
     */
    fun get(
        dslContext: DSLContext,
        id: Long
    ): com.tencent.devops.model.auth.tables.records.TAuthProjectResetRecordRecord? {
        with(TAuthProjectResetRecord.T_AUTH_PROJECT_RESET_RECORD) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }
}
