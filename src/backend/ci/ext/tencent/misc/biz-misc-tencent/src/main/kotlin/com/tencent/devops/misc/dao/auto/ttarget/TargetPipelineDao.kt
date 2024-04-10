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

package com.tencent.devops.misc.dao.auto.ttarget

import com.tencent.devops.model.process.Tables
import com.tencent.devops.model.process.tables.TPipelineBuildDetail
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineResourceRecord
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class TargetPipelineDao {

    fun savePipelineInfo(dslContext: DSLContext, record: TPipelineInfoRecord) {
        val insert = with(Tables.T_PIPELINE_INFO) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                PROJECT_ID,
                VERSION,
                PIPELINE_NAME,
                PIPELINE_DESC,
                CREATE_TIME,
                UPDATE_TIME,
                CHANNEL,
                CREATOR,
                LAST_MODIFY_USER,
                MANUAL_STARTUP,
                ELEMENT_SKIP,
                TASK_COUNT
            ).values(
                record.pipelineId,
                record.projectId,
                record.version,
                record.pipelineName,
                record.pipelineName,
                record.createTime,
                record.updateTime,
                record.channel,
                record.creator,
                record.lastModifyUser,
                record.manualStartup,
                record.elementSkip,
                record.taskCount
            ).onDuplicateKeyUpdate()
                .set(VERSION, record.version)
                .set(PIPELINE_NAME, record.pipelineName)
                .set(PIPELINE_DESC, record.pipelineName)
                .set(CREATE_TIME, record.createTime)
                .set(UPDATE_TIME, record.updateTime)
                .set(CHANNEL, record.channel)
                .set(CREATOR, record.creator)
                .set(LAST_MODIFY_USER, record.lastModifyUser)
                .set(MANUAL_STARTUP, record.manualStartup)
                .set(ELEMENT_SKIP, record.elementSkip)
                .set(TASK_COUNT, record.taskCount)
                .execute()
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("savePipelineInfo|version=${record.version}|pipelineId=${record.pipelineId}|insert=$insert")
        }
    }

    fun savePipelineRes(dslContext: DSLContext, record: TPipelineResourceRecord) {
        val insert = with(Tables.T_PIPELINE_RESOURCE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                VERSION,
                MODEL,
                CREATOR,
                CREATE_TIME
            )
                .values(
                    record.projectId,
                    record.pipelineId,
                    record.version,
                    record.model,
                    record.creator,
                    record.createTime
                )
                .onDuplicateKeyUpdate()
                .set(MODEL, record.model)
                .set(CREATOR, record.creator)
                .set(CREATE_TIME, record.createTime)
                .execute()
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("savePipelineRes|version=${record.version}|pipelineId=${record.pipelineId}|insert=$insert")
        }
    }

    fun savePipelineBuildHistories(dslContext: DSLContext, histories: Collection<TPipelineBuildHistoryRecord>) {
        with(Tables.T_PIPELINE_BUILD_HISTORY) {
            histories.forEach {
                dslContext.insertInto(this)
                    .set(BUILD_ID, it.buildId)
                    .set(BUILD_NUM, it.buildNum)
                    .set(PROJECT_ID, it.projectId)
                    .set(PIPELINE_ID, it.pipelineId)
                    .set(PARENT_BUILD_ID, it.parentBuildId)
                    .set(PARENT_TASK_ID, it.parentTaskId)
                    .set(START_TIME, it.startTime)
                    .set(START_USER, it.startUser)
                    .set(TRIGGER_USER, it.triggerUser)
                    .set(STATUS, it.status)
                    .set(TRIGGER, it.trigger)
                    .set(TASK_COUNT, it.taskCount)
                    .set(FIRST_TASK_ID, it.firstTaskId)
                    .set(CHANNEL, it.channel)
                    .set(VERSION, it.version)
                    .set(QUEUE_TIME, it.queueTime)
                    .set(WEBHOOK_TYPE, it.webhookType)
                    .set(WEBHOOK_INFO, it.webhookInfo)
                    .set(BUILD_MSG, it.buildMsg)
                    .onDuplicateKeyUpdate()
                    .set(BUILD_NUM, it.buildNum)
                    .set(PARENT_BUILD_ID, it.parentBuildId)
                    .set(PARENT_TASK_ID, it.parentTaskId)
                    .set(START_TIME, it.startTime)
                    .set(START_USER, it.startUser)
                    .set(TRIGGER_USER, it.triggerUser)
                    .set(STATUS, it.status)
                    .set(TRIGGER, it.trigger)
                    .set(TASK_COUNT, it.taskCount)
                    .set(FIRST_TASK_ID, it.firstTaskId)
                    .set(CHANNEL, it.channel)
                    .set(VERSION, it.version)
                    .set(QUEUE_TIME, it.queueTime)
                    .set(WEBHOOK_TYPE, it.webhookType)
                    .set(WEBHOOK_INFO, it.webhookInfo)
                    .set(BUILD_MSG, it.buildMsg)
                    .execute()
            }
        }
    }

    fun savePipelineSummary(dslContext: DSLContext, record: TPipelineBuildSummaryRecord) {
        with(Tables.T_PIPELINE_BUILD_SUMMARY) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_NO,
                BUILD_NUM,
                LATEST_BUILD_ID,
                LATEST_START_TIME,
                LATEST_END_TIME,
                LATEST_START_USER,
                LATEST_STATUS,
                LATEST_TASK_COUNT,
                LATEST_TASK_ID,
                LATEST_TASK_NAME,
                FINISH_COUNT,
                RUNNING_COUNT,
                QUEUE_COUNT
            ).values(
                record.projectId,
                record.pipelineId,
                record.buildNo,
                record.buildNum,
                record.latestBuildId,
                record.latestStartTime,
                record.latestEndTime,
                record.latestStartUser,
                record.latestStatus,
                record.latestTaskCount,
                record.latestTaskId,
                record.latestTaskName,
                record.finishCount,
                record.runningCount,
                record.queueCount
            ).onDuplicateKeyUpdate()
                .set(BUILD_NO, record.buildNo)
                .set(BUILD_NUM, record.buildNum)
                .set(LATEST_BUILD_ID, record.latestBuildId)
                .set(LATEST_START_TIME, record.latestStartTime)
                .set(LATEST_END_TIME, record.latestEndTime)
                .set(LATEST_START_USER, record.latestStartUser)
                .set(LATEST_STATUS, record.latestStatus)
                .set(LATEST_TASK_COUNT, record.latestTaskCount)
                .set(LATEST_TASK_ID, record.latestTaskId)
                .set(LATEST_TASK_NAME, record.latestTaskName)
                .set(FINISH_COUNT, record.finishCount)
                .set(RUNNING_COUNT, record.runningCount)
                .set(QUEUE_COUNT, record.queueCount)
                .execute()
        }
    }

    fun savePipelineSetting(dslContext: DSLContext, record: TPipelineSettingRecord) {
        val insert = with(TPipelineSetting.T_PIPELINE_SETTING) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                NAME,
                RUN_LOCK_TYPE,
                DESC,
                SUCCESS_RECEIVER,
                FAIL_RECEIVER,
                SUCCESS_GROUP,
                FAIL_GROUP,
                SUCCESS_TYPE,
                FAIL_TYPE,
                SUCCESS_CONTENT,
                FAIL_CONTENT,
                WAIT_QUEUE_TIME_SECOND,
                MAX_QUEUE_SIZE,
                IS_TEMPLATE,
                MAX_PIPELINE_RES_NUM
            ).values(
                record.projectId,
                record.pipelineId,
                record.name,
                record.runLockType,
                record.desc,
                record.successReceiver,
                record.failReceiver,
                record.successGroup,
                record.failGroup,
                record.successType,
                record.failType,
                record.successContent,
                record.failContent,
                record.waitQueueTimeSecond,
                record.maxQueueSize,
                record.isTemplate,
                record.maxPipelineResNum
            )
                .onDuplicateKeyIgnore().execute()
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("savePipelineSetting|pipelineId=${record.pipelineId}|insert=$insert")
        }
    }

    fun savePipelineBuildDetail(dslContext: DSLContext, record: TPipelineBuildDetailRecord) {
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
                START_USER,
                END_TIME,
                CANCEL_USER
            ).values(
                record.projectId,
                record.buildId,
                record.trigger,
                record.buildNum,
                record.model,
                record.startTime,
                record.status,
                record.startUser,
                record.endTime,
                record.cancelUser
            ).execute()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TargetPipelineDao::class.java)
    }
}
