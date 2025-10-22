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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.model.dispatch.tables.TDispatchRunningJobs
import com.tencent.devops.model.dispatch.tables.records.TDispatchRunningJobsRecord
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class RunningJobsDao {

    fun getAgentRunningJobs(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): TDispatchRunningJobsRecord? {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .fetchOne()
        }
    }

    fun getProjectRunningJobs(
        dslContext: DSLContext,
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name
    ): Result<TDispatchRunningJobsRecord?> {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .fetch()
        }
    }

    fun getProjectRunningJobCount(
        dslContext: DSLContext,
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name
    ): Int {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getSystemRunningJobs(
        dslContext: DSLContext,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ): Result<TDispatchRunningJobsRecord?> {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectFrom(this)
                .where(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .fetch()
        }
    }

    fun getSystemRunningJobCount(
        dslContext: DSLContext,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String
    ): Int {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectCount().from(this)
                .where(VM_TYPE.eq(jobQuotaVmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun insert(
        dslContext: DSLContext,
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        channelCode: String
    ) {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            val now = LocalDateTime.now()

            val preRecord = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .fetchOne()

            if (preRecord != null) {
                dslContext.update(this)
                    .set(CREATED_TIME, now)
                    .where(ID.eq(preRecord.id))
                    .execute()
                return
            } else {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    VM_TYPE,
                    BUILD_ID,
                    VM_SEQ_ID,
                    EXECUTE_COUNT,
                    CHANNEL_CODE,
                    CREATED_TIME
                )
                    .values(
                        projectId,
                        jobQuotaVmType.name,
                        buildId,
                        vmSeqId,
                        executeCount,
                        channelCode,
                        now
                    )
                    .execute()
            }
        }
    }

    fun delete(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ) {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute()
        }
    }

    fun updateAgentStartTime(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ) {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            dslContext.update(this)
                .set(AGENT_START_TIME, LocalDateTime.now())
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute()
        }
    }

    fun getTimeoutRunningJobs(dslContext: DSLContext, days: Long): Result<TDispatchRunningJobsRecord?> {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectFrom(this)
                .where(CREATED_TIME.lessOrEqual(timestampSubDay(days)))
                .fetch()
        }
    }

    fun clearTimeoutRunningJobs(dslContext: DSLContext, days: Long) {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            dslContext.deleteFrom(this)
                .where(CREATED_TIME.lessOrEqual(timestampSubDay(days)))
                .execute()
        }
    }

    fun clearRunningJobs(
        dslContext: DSLContext,
        projectId: String,
        vmType: JobQuotaVmType,
        channelCode: String,
        createTime: LocalDateTime
    ) {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VM_TYPE.eq(vmType.name))
                .and(CHANNEL_CODE.eq(channelCode))
                .and(CREATED_TIME.lessOrEqual(createTime))
                .execute()
        }
    }

    fun getProject(dslContext: DSLContext): Result<Record1<String>>? {
        with(TDispatchRunningJobs.T_DISPATCH_RUNNING_JOBS) {
            return dslContext.selectDistinct(PROJECT_ID).from(this).fetch()
        }
    }

    private fun timestampSubDay(day: Long): Field<LocalDateTime> {
        return DSL.field("date_sub(NOW(), interval $day day)",
            LocalDateTime::class.java)
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field("timestampdiff({0}, {1}, NOW()) as runningTime",
            Int::class.java, DSL.keyword(part.toSQL()), t1)
    }
}
