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

import com.tencent.devops.model.process.Tables
import com.tencent.devops.model.process.tables.records.TPipelinePauseValueRecord
import com.tencent.devops.process.engine.pojo.PipelinePauseValue
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

@Suppress("ALL")
@Repository
class PipelinePauseValueDao {

    fun save(dslContext: DSLContext, pipelinePauseValue: PipelinePauseValue) {
        with(Tables.T_PIPELINE_PAUSE_VALUE) {
            dslContext.insertInto(this)
                .set(PROJECT_ID, pipelinePauseValue.projectId)
                .set(BUILD_ID, pipelinePauseValue.buildId)
                .set(TASK_ID, pipelinePauseValue.taskId)
                .set(DEFAULT_VALUE, pipelinePauseValue.defaultValue)
                .set(NEW_VALUE, pipelinePauseValue.newValue)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(EXECUTE_COUNT, pipelinePauseValue.executeCount)
                .set(CREATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        taskId: String,
        executeCount: Int?
    ): TPipelinePauseValueRecord? {
        return with(Tables.T_PIPELINE_PAUSE_VALUE) {
            val query = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId))
                .and(PROJECT_ID.eq(projectId))
            if (executeCount != null) {
                query.and(EXECUTE_COUNT.eq(executeCount))
            } else {
                query.and(EXECUTE_COUNT.eq(1).or(EXECUTE_COUNT.isNull))
            }
            query.fetchAny()
        }
    }

    fun convert(t: TPipelinePauseValueRecord?): PipelinePauseValue? {
        return if (t != null) {
            PipelinePauseValue(
                projectId = t.projectId,
                buildId = t.buildId,
                taskId = t.taskId,
                defaultValue = t.defaultValue,
                newValue = t.newValue,
                executeCount = t.executeCount
            )
        } else {
            null
        }
    }
}
