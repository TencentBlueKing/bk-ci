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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_BUILD_RECORD_FOR_CORRESPONDING_SUB_PIPELINE
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SubPipelineStatusService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {

    companion object {
        // 子流水线启动过期时间900分钟
        private const val SUBPIPELINE_STATUS_START_EXPIRED = 54000L
        // 子流水线完成过期时间10分钟
        private const val SUBPIPELINE_STATUS_FINISH_EXPIRED = 600L
        private val logger = LoggerFactory.getLogger(SubPipelineStatusService::class.java)
    }

    fun onStart(buildId: String) {
        redisOperation.set(
            key = getSubPipelineStatusKey(buildId),
            value = JsonUtil.toJson(
                SubPipelineStatus(status = BuildStatus.RUNNING.name)
            ),
            expiredInSecond = SUBPIPELINE_STATUS_START_EXPIRED
        )
    }

    /**
     * 异步启动子流水线
     */
    fun onAsyncStart(event: PipelineBuildStartEvent) {
        with(event) {
            try {
                updateParentPipelineTaskStatus(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    asyncStatus = "start"
                )
            } catch (ignored: Exception) {
                logger.warn("fail to update parent pipeline task status", ignored)
            }
        }
    }

    fun onFinish(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            // 不是流水线启动
            if (triggerType != StartType.PIPELINE.name) {
                return
            }
            updateParentPipelineTaskStatus(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                asyncStatus = "finish"
            )
            // 子流水线是异步启动的，不需要缓存状态
            if (redisOperation.get(getSubPipelineStatusKey(buildId)) != null) {
                redisOperation.set(
                    key = getSubPipelineStatusKey(buildId),
                    value = JsonUtil.toJson(getSubPipelineStatusFromDB(event.projectId, buildId)),
                    expiredInSecond = SUBPIPELINE_STATUS_FINISH_EXPIRED
                )
            }
        }
    }

    private fun updateParentPipelineTaskStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        asyncStatus: String
    ) {
        // 父流水线相关信息
        val keys = setOf(
            PIPELINE_START_PARENT_PROJECT_ID,
            PIPELINE_START_PARENT_PIPELINE_ID,
            PIPELINE_START_PARENT_BUILD_ID,
            PIPELINE_START_PARENT_BUILD_TASK_ID
        )
        val buildVariables = pipelineRuntimeService.getBuildVariableService(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            keys = keys
        )
        keys.forEach {
            // 存在异常值，则直接返回
            if (buildVariables[it].isNullOrBlank()) {
                logger.warn(
                    "The parent pipeline status cannot be updated, " +
                            "because an abnormal variable exists[$buildVariables]"
                )
                return
            }
        }
        pipelineRuntimeService.getBuildInfo(
            projectId = buildVariables[PIPELINE_START_PARENT_PROJECT_ID]!!,
            pipelineId = buildVariables[PIPELINE_START_PARENT_PIPELINE_ID]!!,
            buildId = buildVariables[PIPELINE_START_PARENT_BUILD_ID]!!
        )?.let {
            logger.info("start update parent pipeline asyncStatus[$asyncStatus]|${it.projectId}|${it.pipelineId}|" +
                    "${it.buildId}|${it.executeCount}|")
            pipelineRuntimeService.updateAsyncStatus(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                buildId = it.buildId,
                taskId = buildVariables[PIPELINE_START_PARENT_BUILD_TASK_ID]!!,
                executeCount = it.executeCount ?: 1,
                asyncStatus = asyncStatus
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "updateTaskStatus",
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    userId = it.startUser,
                    buildId = it.buildId,
                    refreshTypes = RefreshType.RECORD.binary,
                    executeCount = it.executeCount ?: 1
                )
            )
        }
    }

    private fun getSubPipelineStatusFromDB(projectId: String, buildId: String): SubPipelineStatus {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        return if (buildInfo != null) {
            val status: BuildStatus = when {
                buildInfo.isSuccess() && buildInfo.isStageSuccess() -> BuildStatus.SUCCEED
                buildInfo.isFinish() -> buildInfo.status
                buildInfo.isReadyToRun() -> BuildStatus.RUNNING // QUEUE状态
                buildInfo.isStageSuccess() -> BuildStatus.RUNNING // stage 特性， 未结束，只是卡在Stage审核中
                else -> buildInfo.status
            }
            SubPipelineStatus(
                status = status.name
            )
        } else {
            SubPipelineStatus(
                status = BuildStatus.FAILED.name,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = I18nUtil.getCodeLanMessage(ERROR_NO_BUILD_RECORD_FOR_CORRESPONDING_SUB_PIPELINE)
            )
        }
    }

    fun getSubPipelineStatus(projectId: String, buildId: String): SubPipelineStatus {
        val subPipelineStatusStr = redisOperation.get(getSubPipelineStatusKey(buildId))
        return if (subPipelineStatusStr.isNullOrBlank()) {
            getSubPipelineStatusFromDB(projectId, buildId)
        } else {
            val redisSubPipelineStatus = JsonUtil.to(subPipelineStatusStr, SubPipelineStatus::class.java)
            // 如果状态完成,子流水线插件就不会再调用,从redis中删除key
            if (BuildStatus.parse(redisSubPipelineStatus.status).isFinish()) {
                redisOperation.delete(getSubPipelineStatusKey(buildId))
            }
            redisSubPipelineStatus
        }
    }

    private fun getSubPipelineStatusKey(buildId: String): String {
        return "subpipeline:build:$buildId:status"
    }
}
