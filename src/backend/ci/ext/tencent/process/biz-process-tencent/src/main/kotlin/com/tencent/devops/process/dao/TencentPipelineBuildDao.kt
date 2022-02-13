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
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.model.process.Tables
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildStageRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class TencentPipelineBuildDao {

    fun listScanPipelineBuildList(
        dslContext: DSLContext,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(Tables.T_PIPELINE_BUILD_HISTORY) {
            val where = dslContext.selectFrom(this).where(CHANNEL.eq(ChannelCode.GONGFENGSCAN.name))
            if (status != null && status.isNotEmpty()) { // filterNotNull不能删
                where.and(STATUS.`in`(status.map { it.ordinal }))
            }
            if (trigger != null && trigger.isNotEmpty()) { // filterNotNull不能删
                where.and(TRIGGER.`in`(trigger.map { it.name }))
            }
            if (queueTimeStartTime != null && queueTimeStartTime > 0) {
                where.and(QUEUE_TIME.ge(Timestamp(queueTimeStartTime).toLocalDateTime()))
            }
            if (queueTimeEndTime != null && queueTimeEndTime > 0) {
                where.and(QUEUE_TIME.le(Timestamp(queueTimeEndTime).toLocalDateTime()))
            }
            if (startTimeStartTime != null && startTimeStartTime > 0) {
                where.and(START_TIME.ge(Timestamp(startTimeStartTime).toLocalDateTime()))
            }
            if (startTimeEndTime != null && startTimeEndTime > 0) {
                where.and(START_TIME.le(Timestamp(startTimeEndTime).toLocalDateTime()))
            }
            if (endTimeStartTime != null && endTimeStartTime > 0) {
                where.and(END_TIME.ge(Timestamp(endTimeStartTime).toLocalDateTime()))
            }
            if (endTimeEndTime != null && endTimeEndTime > 0) {
                where.and(END_TIME.le(Timestamp(endTimeEndTime).toLocalDateTime()))
            }
            where.orderBy(END_TIME.desc())
                .fetch()
        }
    }

    fun listCheckOutErrorStage(
        dslContext: DSLContext,
        stageTimeoutDays: Long
    ): Collection<TPipelineBuildStageRecord> {
        return with(Tables.T_PIPELINE_BUILD_STAGE) {
            dslContext.selectFrom(this)
                .where(STATUS.eq(BuildStatus.RUNNING.ordinal).and(END_TIME.isNotNull)
                    .and(CHECK_OUT.notLike("%QUALITY_CHECK_WAIT%")))
                .or(STATUS.eq(BuildStatus.RUNNING.ordinal)
                    .and(CHECK_OUT.like("%QUALITY_CHECK_WAIT%"))
                    .and(END_TIME.lt(LocalDateTime.now().minusDays(stageTimeoutDays)))
                )
                .fetch()
        }
    }

    fun listRunningErrorBuild(
        dslContext: DSLContext,
        buildTimeoutDays: Long
    ): Collection<TPipelineBuildHistoryRecord> {
        return with(Tables.T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(STATUS.eq(BuildStatus.RUNNING.ordinal)
                    .and(START_TIME.lt(LocalDateTime.now().minusDays(buildTimeoutDays)))
                )
                .fetch()
        }
    }
}
