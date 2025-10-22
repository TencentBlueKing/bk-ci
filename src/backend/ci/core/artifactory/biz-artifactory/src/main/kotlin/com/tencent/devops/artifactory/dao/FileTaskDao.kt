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

package com.tencent.devops.artifactory.dao

import com.tencent.devops.model.artifactory.tables.TFileTask
import com.tencent.devops.model.artifactory.tables.records.TFileTaskRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class FileTaskDao {

    fun addFileTaskInfo(
        dslContext: DSLContext,
        taskId: String,
        fileType: String,
        filePath: String,
        machineIp: String,
        localPath: String,
        status: Short,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Int {
        val nowTime = LocalDateTime.now()
        with(TFileTask.T_FILE_TASK) {
            return dslContext.insertInto(
                this,
                TASK_ID,
                FILE_TYPE,
                FILE_PATH,
                MACHINE_IP,
                LOCAL_PATH,
                STATUS,
                USER_ID,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    taskId,
                    fileType,
                    filePath,
                    machineIp,
                    localPath,
                    status,
                    userId,
                    projectId,
                    pipelineId,
                    buildId,
                    nowTime,
                    nowTime
                ).execute()
        }
    }

    fun updateFileTaskStatus(
        dslContext: DSLContext,
        taskId: String,
        status: Short
    ): Int {
        val nowTime = LocalDateTime.now()
        with(TFileTask.T_FILE_TASK) {
            return dslContext.update(this)
                .set(STATUS, status)
                .set(UPDATE_TIME, nowTime)
                .where(TASK_ID.eq(taskId)).execute()
        }
    }

    fun listHistoryFileTaskInfo(
        dslContext: DSLContext,
        status: Short?,
        updateTime: LocalDateTime?,
        limit: Int?
    ): Result<TFileTaskRecord>? {
        with(TFileTask.T_FILE_TASK) {
            val conditions = mutableListOf<Condition>()
            if (status != null) {
                conditions.add(STATUS.eq(status))
            }
            if (updateTime != null) {
                conditions.add(UPDATE_TIME.lessOrEqual(updateTime))
            }
            val query = dslContext.selectFrom(this).where(conditions)
            if (limit != null) {
                return query.limit(limit).fetch()
            } else {
                return query.fetch()
            }
        }
    }

    fun getFileTaskInfo(dslContext: DSLContext, taskId: String): TFileTaskRecord? {
        return with(TFileTask.T_FILE_TASK) {
            dslContext.selectFrom(this).where(TASK_ID.eq(taskId)).fetchAny()
        }
    }

    fun deleteFileTaskInfo(dslContext: DSLContext, taskId: String): Int {
        return with(TFileTask.T_FILE_TASK) {
            dslContext.deleteFrom(this).where(TASK_ID.eq(taskId)).execute()
        }
    }

    fun deleteFileTaskInfo(dslContext: DSLContext, taskIds: Collection<String>): Int {
        return with(TFileTask.T_FILE_TASK) {
            dslContext.deleteFrom(this).where(TASK_ID.`in`(taskIds)).execute()
        }
    }
}
