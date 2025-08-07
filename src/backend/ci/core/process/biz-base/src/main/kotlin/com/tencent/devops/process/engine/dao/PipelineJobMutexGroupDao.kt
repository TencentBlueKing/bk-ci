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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineJobMutexGroup
import com.tencent.devops.model.process.tables.records.TPipelineJobMutexGroupRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class PipelineJobMutexGroupDao {

    fun getByProjectId(dslContext: DSLContext, projectId: String): Result<TPipelineJobMutexGroupRecord>? {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun insert(dslContext: DSLContext, projectId: String, jobMutexGroupName: String): Boolean {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            return dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(JOB_MUTEX_GROUP_NAME, jobMutexGroupName)
                .execute() > 0
        }
    }

    fun create(dslContext: DSLContext, projectId: String, jobMutexGroupName: String): Boolean {
        if (jobMutexGroupName.isBlank()) {
            return false
        }
        // 当存在的时候不再插入，不存在的时候插入一条新的。
        return if (exit(dslContext, projectId, jobMutexGroupName)) {
            true
        } else {
            insert(dslContext, projectId, jobMutexGroupName)
        }
    }

    fun exit(dslContext: DSLContext, projectId: String, jobMutexGroupName: String): Boolean {
        with(TPipelineJobMutexGroup.T_PIPELINE_JOB_MUTEX_GROUP) {
            return dslContext.selectFrom(this)
                .where(
                    PROJECT_ID.eq(projectId),
                    JOB_MUTEX_GROUP_NAME.eq(jobMutexGroupName)
                ).fetch().isNotEmpty
        }
    }
}
