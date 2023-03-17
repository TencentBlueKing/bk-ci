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

package com.tencent.devops.process.engine.service.record

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.BUILD_CANCELED
import com.tencent.devops.common.api.constant.BUILD_COMPLETED
import com.tencent.devops.common.api.constant.BUILD_FAILED
import com.tencent.devops.common.api.constant.BUILD_REVIEWING
import com.tencent.devops.common.api.constant.BUILD_RUNNING
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.engine.control.lock.PipelineBuildRecordLock
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("LongParameterList", "MagicNumber", "ReturnCount", "ComplexMethod")
open class BaseBuildRecordService(
    private val dslContext: DSLContext,
    private val buildRecordModelDao: BuildRecordModelDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val stageTagService: StageTagService
) {

    protected fun update(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        cancelUser: String? = null,
        operation: String = "",
        refreshOperation: () -> Unit
    ) {
        val watcher = Watcher(id = "updateRecord#$buildId#$operation")
        var message = "nothing"
        val lock = PipelineBuildRecordLock(redisOperation, buildId, executeCount)
        try {
            watcher.start("lock")
            lock.lock()

            watcher.start("getRecord")
            val record = buildRecordModelDao.getRecord(
                dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount
            ) ?: run {
                message = "Will not update"
                return
            }

            watcher.start("refreshOperation")
            refreshOperation()
            watcher.stop()

            watcher.start("dispatchEvent")
            pipelineDetailChangeEvent(projectId, pipelineId, buildId, record.startUser, executeCount)

            watcher.start("updatePipelineRecord")
            val (change, finalStatus) = takeBuildStatus(record, buildStatus)
            if (!change) {
                message = "Will not update"
                return
            }
            buildRecordModelDao.updateRecord(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount,
                buildStatus = finalStatus,
                modelVar = emptyMap(), // 暂时没有变量，保留修改可能
                startTime = null,
                endTime = null,
                errorInfoList = null,
                cancelUser = cancelUser // 系统行为导致的取消状态(仅当在取消状态时，还没有设置过取消人，才默认为System)
                    ?: if (buildStatus.isCancel() && record.cancelUser.isNullOrBlank()) "System" else null,
                timestamps = null
            )
            message = "Will not update"
        } catch (ignored: Throwable) {
            message = ignored.message ?: ""
            logger.warn("[$buildId]| Fail to update the build record: ${ignored.message}", ignored)
        } finally {
            lock.unlock()
            watcher.stop()
            logger.info("[$buildId|$buildStatus]|$operation|update_detail_record| $message")
            LogUtils.printCostTimeWE(watcher)
        }
        return
    }

    private fun takeBuildStatus(
        record: BuildRecordModel,
        buildStatus: BuildStatus
    ): Pair<Boolean, BuildStatus> {
        val oldStatus = BuildStatus.parse(record.status)
        return if (!oldStatus.isFinish()) {
            (oldStatus != buildStatus) to buildStatus
        } else {
            false to oldStatus
        }
    }

    private fun pipelineDetailChangeEvent(
        projectId: String,
        pipelineId: String,
        buildId: String,
        startUser: String,
        executeCount: Int
    ) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "pauseTask",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = startUser,
                buildId = buildId,
                executeCount = executeCount,
                refreshTypes = RefreshType.RECORD.binary
            )
        )
    }

    protected fun fetchHistoryStageStatus(
        recordStages: List<BuildRecordStage>,
        buildStatus: BuildStatus,
        timeCost: BuildRecordTimeCost? = null,
        reviewers: List<String>? = null,
        errorMsg: String? = null,
        cancelUser: String? = null
    ): List<BuildStageStatus> {
        val stageTagMap: Map<String, String> by lazy {
            stageTagService.getAllStageTag().data!!.associate { it.id to it.stageTagName }
        }
        // 更新Stage状态至BuildHistory
        val (statusMessage, reason) = if (buildStatus == BuildStatus.REVIEWING) {
            Pair(BUILD_REVIEWING, reviewers?.joinToString(","))
        } else if (buildStatus.isFailure()) {
            Pair(BUILD_FAILED, errorMsg ?: buildStatus.name)
        } else if (buildStatus.isCancel()) {
            Pair(BUILD_CANCELED, cancelUser)
        } else if (buildStatus.isSuccess()) {
            Pair(BUILD_COMPLETED, null)
        } else {
            Pair(BUILD_RUNNING, null)
        }
        return recordStages.map {
            BuildStageStatus(
                stageId = it.stageId,
                name = it.stageVar[Stage::name.name]?.toString() ?: it.stageId,
                status = it.status,
                startEpoch = it.stageVar[Stage::startEpoch.name]?.toString()?.toLong(),
                elapsed = it.stageVar[Stage::elapsed.name]?.toString()?.toLong(),
                timeCost = timeCost,
                tag = it.stageVar[Stage::tag.name]?.let { tags ->
                    JsonUtil.anyTo(tags, object : TypeReference<List<String>>() {})
                        .map { tag -> stageTagMap.getOrDefault(tag, "null") }
                },
                // #6655 利用stageStatus中的第一个stage传递构建的状态信息
                showMsg = if (it.stageId == StageBuildRecordService.TRIGGER_STAGE) {
                    MessageCodeUtil.getCodeLanMessage(statusMessage) + (reason?.let { ": $reason" } ?: "")
                } else null
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BaseBuildRecordService::class.java)

        fun mergeTimestamps(
            newTimestamps: Map<BuildTimestampType, BuildRecordTimeStamp>,
            oldTimestamps: Map<BuildTimestampType, BuildRecordTimeStamp>
        ): MutableMap<BuildTimestampType, BuildRecordTimeStamp> {
            // 针对各时间戳的开始结束时间分别写入，避免覆盖
            val result = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()
            result.putAll(oldTimestamps)
            newTimestamps.forEach { (type, new) ->
                val old = oldTimestamps[type]
                result[type] = if (old != null) {
                    // 如果时间戳已存在，则将新的值覆盖旧的值
                    BuildRecordTimeStamp(
                        startTime = new.startTime ?: old.startTime,
                        endTime = new.endTime ?: old.endTime
                    )
                } else {
                    // 如果时间戳不存在，则直接新增
                    new
                }
            }
            return result
        }
    }
}
