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

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class BuildDetailDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        startUser: String,
        startType: StartType?,
        buildNum: Int?,
        model: String,
        buildStatus: BuildStatus = BuildStatus.RUNNING
    ) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                BUILD_ID,
                TRIGGER,
                BUILD_NUM,
                MODEL,
                START_TIME,
                STATUS,
                START_USER
            ).values(
                projectId,
                buildId,
                startType?.name,
                buildNum ?: 0,
                model,
                LocalDateTime.now(),
                buildStatus.name,
                startUser
            ).execute()
        }
    }

    fun updateBuildCancelUser(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        cancelUser: String
    ) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            dslContext.update(this)
                .set(CANCEL_USER, cancelUser)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        model: String?,
        buildStatus: BuildStatus,
        cancelUser: String? = null
    ): Int {
        val count = with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            val update = dslContext.update(this)
                .set(STATUS, buildStatus.name)
            if (!model.isNullOrBlank()) {
                update.set(MODEL, model)
            }
            if (buildStatus.isFinish()) {
                update.set(END_TIME, LocalDateTime.now())
            }
            if (cancelUser != null) {
                update.set(CANCEL_USER, cancelUser)
            }
            update.where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId))).execute()
        }
        logger.info("[$buildId]|Update the build|status=$buildStatus|ret=$count")
        return count
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        buildStatus: BuildStatus,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null
    ) {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            val execute = dslContext.update(this).set(STATUS, buildStatus.name)
            if (startTime != null) {
                execute.set(START_TIME, startTime)
            }
            if (endTime != null) {
                execute.set(END_TIME, endTime)
            } else if (buildStatus.isFinish()) {
                execute.set(END_TIME, LocalDateTime.now())
            }
            execute.where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId))).execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): TPipelineBuildDetailRecord? {
        with(TPipelineBuildDetail.T_PIPELINE_BUILD_DETAIL) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(BUILD_ID.eq(buildId)))
                .fetchAny()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildDetailDao::class.java)
    }
}
