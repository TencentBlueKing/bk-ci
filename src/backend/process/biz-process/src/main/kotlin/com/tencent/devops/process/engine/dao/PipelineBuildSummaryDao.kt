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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_SUMMARY
import com.tencent.devops.model.process.Tables.T_PIPELINE_INFO
import com.tencent.devops.model.process.Tables.T_PIPELINE_SETTING
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildSummaryDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildNo: BuildNo?
    ) {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_NO
            ).values(projectId, pipelineId, buildNo?.buildNo ?: 0).execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.delete(this)
                .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    fun get(dslContext: DSLContext, pipelineId: String): TPipelineBuildSummaryRecord? {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchAny()
        }
    }

    fun getBuildNo(dslContext: DSLContext, pipelineId: String): Int {
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.select(BUILD_NO).from(this)
                .where(PIPELINE_ID.eq(pipelineId)).fetchOne(BUILD_NO, Int::class.java)
        }
    }

    fun updateBuildNo(dslContext: DSLContext, pipelineId: String, buildNo: Int) {

        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(BUILD_NO, buildNo)
                .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    fun updateBuildNum(dslContext: DSLContext, pipelineId: String, buildNum: Int = 0): Int {

        with(T_PIPELINE_BUILD_SUMMARY) {
            if (buildNum == 0)
                dslContext.update(this)
                    .set(BUILD_NUM, BUILD_NUM + 1)
                    .where(PIPELINE_ID.eq(pipelineId)).execute()
            else
                dslContext.update(this)
                    .set(BUILD_NUM, buildNum)
                    .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
        return with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.select(BUILD_NUM)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)
        }
    }

    fun listPipelineInfoBuildSummary(
        dslContext: DSLContext,
        projectId: String,
        channelCode: ChannelCode,
        pipelineIds: Collection<String>? = null
    ): Result<out Record> {
        val where = dslContext.select(
            T_PIPELINE_INFO.PIPELINE_ID,
            T_PIPELINE_INFO.PROJECT_ID,
            T_PIPELINE_INFO.VERSION,
            T_PIPELINE_INFO.PIPELINE_NAME,
            T_PIPELINE_INFO.CREATE_TIME,
            T_PIPELINE_INFO.UPDATE_TIME,
            T_PIPELINE_INFO.CHANNEL,
            T_PIPELINE_INFO.CREATOR,
            T_PIPELINE_INFO.MANUAL_STARTUP,
            T_PIPELINE_INFO.ELEMENT_SKIP,
            T_PIPELINE_INFO.TASK_COUNT,
            T_PIPELINE_SETTING.DESC,
            T_PIPELINE_SETTING.RUN_LOCK_TYPE,
            T_PIPELINE_BUILD_SUMMARY.BUILD_NUM,
            T_PIPELINE_BUILD_SUMMARY.BUILD_NO,
            T_PIPELINE_BUILD_SUMMARY.FINISH_COUNT,
            T_PIPELINE_BUILD_SUMMARY.RUNNING_COUNT,
            T_PIPELINE_BUILD_SUMMARY.QUEUE_COUNT,
            T_PIPELINE_BUILD_SUMMARY.LATEST_BUILD_ID,
            T_PIPELINE_BUILD_SUMMARY.LATEST_TASK_COUNT,
            T_PIPELINE_BUILD_SUMMARY.LATEST_START_USER,
            T_PIPELINE_BUILD_SUMMARY.LATEST_START_TIME,
            T_PIPELINE_BUILD_SUMMARY.LATEST_END_TIME,
            T_PIPELINE_BUILD_SUMMARY.LATEST_TASK_NAME,
            T_PIPELINE_BUILD_SUMMARY.LATEST_STATUS
        )
            .from(T_PIPELINE_INFO)
            .innerJoin(
                T_PIPELINE_SETTING
                    .innerJoin(
                        T_PIPELINE_BUILD_SUMMARY
                    ).on(T_PIPELINE_SETTING.PIPELINE_ID.eq(T_PIPELINE_BUILD_SUMMARY.PIPELINE_ID))
            ).on(T_PIPELINE_INFO.PIPELINE_ID.eq(T_PIPELINE_SETTING.PIPELINE_ID))
            .where(T_PIPELINE_INFO.PROJECT_ID.eq(projectId))
            .and(T_PIPELINE_INFO.CHANNEL.eq(channelCode.name))
            .and(T_PIPELINE_INFO.DELETE.eq(false))

        if (pipelineIds != null && pipelineIds.isNotEmpty()) {
            where.and(T_PIPELINE_INFO.PIPELINE_ID.`in`(pipelineIds))
        }
        return where.fetch()
    }

    /**
     * 1：新构建时都先进入排队，计数
     */
    fun updateQueueCount(dslContext: DSLContext, pipelineId: String, queueIncrement: Int = 1) {
        with(T_PIPELINE_BUILD_SUMMARY) {
            dslContext.update(this)
                .set(QUEUE_COUNT, QUEUE_COUNT + queueIncrement)
                .where(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    /**
     * 2：写最新一次运行中的构建信息
     */
    fun startLatestRunningBuild(
        dslContext: DSLContext,
        latestRunningBuild: LatestRunningBuild
    ): Int {
        return with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                dslContext.update(this)
                    .set(LATEST_BUILD_ID, buildId)
                    .set(LATEST_TASK_COUNT, taskCount)
                    .set(LATEST_START_USER, userId)
                    .set(QUEUE_COUNT, QUEUE_COUNT - 1)
                    .set(RUNNING_COUNT, RUNNING_COUNT + 1)
                    .set(LATEST_START_TIME, LocalDateTime.now())
                    .where(PIPELINE_ID.eq(pipelineId)).execute()
                dslContext.update(this)
                    .set(LATEST_STATUS, status.ordinal) // 一般必须是RUNNING
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(LATEST_BUILD_ID.eq(buildId)).execute()
            }
        }
    }

    /**
     * 更新运行中的任务信息摘要
     */
    fun updateCurrentBuildTask(dslContext: DSLContext, latestRunningBuild: LatestRunningBuild) {
        with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                dslContext.update(this)
                    .set(LATEST_TASK_ID, currentTaskId)
                    .set(LATEST_TASK_NAME, currentTaskName)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(LATEST_BUILD_ID.eq(buildId)).execute()
            }
        }
    }

    /**
     * 3：结束运行记录
     */
    fun finishLatestRunningBuild(dslContext: DSLContext, latestRunningBuild: LatestRunningBuild) {
        val count = with(latestRunningBuild) {
            with(T_PIPELINE_BUILD_SUMMARY) {
                dslContext.update(this)
                    .set(LATEST_STATUS, status.ordinal) // 不一定是FINISH，也有可能其它失败的status
                    .set(LATEST_END_TIME, LocalDateTime.now()) // 结束时间
                    .set(LATEST_TASK_ID, "") // 结束时清空
                    .set(LATEST_TASK_NAME, "") // 结束时清空
                    .set(RUNNING_COUNT, RUNNING_COUNT - 1)
                    .set(FINISH_COUNT, FINISH_COUNT + 1)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(LATEST_BUILD_ID.eq(buildId)).execute()
            }
        }
        // 没更新到，可能是因为他不是当前最新一次构建，那么要做的一件事是对finishCount值做加1，同时runningCount值减1
        if (count == 0) {
            with(latestRunningBuild) {
                with(T_PIPELINE_BUILD_SUMMARY) {
                    dslContext.update(this)
                        .set(FINISH_COUNT, FINISH_COUNT + 1)
                        .set(RUNNING_COUNT, RUNNING_COUNT - 1)
                        .where(PIPELINE_ID.eq(pipelineId)).execute()
                }
            }
        }
    }

//    fun resetTransferRecord(dslContext: DSLContext, pipelineId: String, startTime: Date?, endTime: Date?, historyBuildCount: Int) {
//        with(T_PIPELINE_BUILD_SUMMARY) {
//            dslContext.update(this)
//                    .set(FINISH_COUNT, historyBuildCount)
//                    .set(LATEST_START_TIME, if (startTime == null) null else LocalDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault()))
//                    .set(LATEST_END_TIME, if (endTime == null) null else LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault()))
//                    .set(QUEUE_COUNT, 0)
//                    .where(PIPELINE_ID.eq(pipelineId)).execute()
//        }
//    }
}
