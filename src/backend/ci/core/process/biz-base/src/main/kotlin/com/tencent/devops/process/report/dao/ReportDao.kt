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

package com.tencent.devops.process.report.dao

import com.tencent.devops.model.process.tables.TPipelineBuildTask
import com.tencent.devops.model.process.tables.TReport
import com.tencent.devops.model.process.tables.records.TReportRecord
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class ReportDao {
    fun getAtomInfo(
        dslContext: DSLContext,
        buildId: String,
        taskId: String
    ): Record2<String, String>? {
        with(TPipelineBuildTask.T_PIPELINE_BUILD_TASK) {
            return dslContext.select(ATOM_CODE, TASK_NAME).from(this)
                .where(BUILD_ID.eq(buildId))
                .and(TASK_ID.eq(taskId))
                .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        indexFile: String,
        name: String,
        type: String,
        atomCode: String,
        taskName: String,
        id: Long? = null
    ): Long {
        val now = LocalDateTime.now()
        with(TReport.T_REPORT) {
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                ELEMENT_ID,
                INDEX_FILE,
                NAME,
                TYPE,
                CREATE_TIME,
                UPDATE_TIME,
                ATOM_CODE,
                TASK_NAME,
                ID
            ).values(
                projectId,
                pipelineId,
                buildId,
                elementId,
                indexFile,
                name,
                type,
                now,
                now,
                atomCode,
                taskName,
                id
            )
                .returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun exists(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        name: String
    ): Boolean {
        with(TReport.T_REPORT) {
            val count = dslContext.select(DSL.count(ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .and(NAME.eq(name))
                .fetchOne(0, Int::class.java)

            return count != null && count > 0
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        indexFile: String,
        name: String,
        atomCode: String,
        taskName: String,
        type: ReportTypeEnum
    ): Int {
        with(TReport.T_REPORT) {
            return dslContext.update(this)
                .set(ELEMENT_ID, elementId)
                .set(INDEX_FILE, indexFile)
                .set(NAME, name)
                .set(ATOM_CODE, atomCode)
                .set(TASK_NAME, taskName)
                .set(TYPE, type.name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .and(NAME.eq(name))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String): Result<TReportRecord> {
        with(TReport.T_REPORT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .fetch()
        }
    }
}
