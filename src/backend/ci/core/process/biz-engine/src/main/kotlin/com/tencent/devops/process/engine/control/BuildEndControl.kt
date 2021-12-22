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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorCode.PLUGIN_DEFAULT_ERROR
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildNoLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildStartLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.utils.PIPELINE_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 构建控制器
 * @version 1.0
 */
@Service
class BuildEndControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val buildLogPrinter: BuildLogPrinter
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildEndControl::class.java)
    }

    fun handle(event: PipelineBuildFinishEvent) {
        val watcher = Watcher(id = "ENGINE|BuildEnd|${event.traceId}|${event.buildId}|Job#${event.status}")
        try {
            with(event) {

                val buildIdLock = BuildIdLock(redisOperation, buildId)
                try {
                    watcher.start("BuildIdLock")
                    buildIdLock.lock()
                    watcher.start("finish")
                    finish()
                    watcher.stop()
                } catch (ignored: Exception) {
                    LOG.warn("ENGINE|$buildId|$source|BUILD_FINISH_ERR|build finish fail: $ignored", ignored)
                } finally {
                    buildIdLock.unlock()
                }

                val buildStartLock = PipelineBuildStartLock(redisOperation, pipelineId)
                try {
                    watcher.start("PipelineBuildStartLock")
                    buildStartLock.lock()
                    watcher.start("popNextBuild")
                    popNextBuild()
                    watcher.stop()
                } finally {
                    buildStartLock.unlock()
                }
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }

    private fun PipelineBuildFinishEvent.finish() {

        // 将状态设置正确
        val buildStatus = BuildStatusSwitcher.pipelineStatusMaker.finish(status)

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)

        // 当前构建整体的状态，可能是运行中，也可能已经失败
        // 已经结束的构建，不再受理，抛弃消息 #5090 STAGE_SUCCESS 状态的也可能是已经处理完成
        if (buildInfo == null || buildInfo.isFinish()) {
            LOG.info("ENGINE|$buildId|$source|BUILD_FINISH_REPEAT_EVENT|STATUS=${buildInfo?.status}| abandon!")
            return
        }

        LOG.info("ENGINE|$buildId|$source|BUILD_FINISH|$pipelineId|es=$status|bs=${buildInfo.status}")

        fixTask(buildInfo)

        // 记录本流水线最后一次构建的状态
        pipelineRuntimeService.finishLatestRunningBuild(
            latestRunningBuild = LatestRunningBuild(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                userId = buildInfo.startUser, status = buildStatus, taskCount = buildInfo.taskCount,
                buildNum = buildInfo.buildNum
            ),
            currentBuildStatus = buildInfo.status,
            errorInfoList = buildInfo.errorInfoList
        )

        // 更新buildNo
        if (!buildStatus.isCancel() && !buildStatus.isFailure()) {
            setBuildNoWhenBuildSuccess(pipelineId = pipelineId, buildId = buildId)
        }

        // 设置状态
        val allStageStatus = pipelineBuildDetailService.buildEnd(
            buildId = buildId,
            buildStatus = buildStatus
        )

        pipelineRuntimeService.updateBuildHistoryStageState(buildId, allStageStatus)

        // 广播结束事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishBroadCastEvent(
                source = "build_finish_$buildId", projectId = projectId, pipelineId = pipelineId,
                userId = userId, buildId = buildId, status = buildStatus.name,
                startTime = buildInfo.startTime, endTime = buildInfo.endTime, triggerType = buildInfo.trigger,
                errorInfoList = if (buildInfo.errorInfoList != null) {
                    JsonUtil.toJson(buildInfo.errorInfoList!!)
                } else null
            ),
            PipelineBuildStatusBroadCastEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                actionType = ActionType.END
            ),
            PipelineBuildWebSocketPushEvent(
                source = "pauseTask",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                refreshTypes = RefreshType.HISTORY.binary
            )
        )

        // 记录日志
        buildLogPrinter.stopLog(buildId = buildId, tag = "", jobId = null)
    }

    private fun setBuildNoWhenBuildSuccess(pipelineId: String, buildId: String) {
        val model = pipelineBuildDetailService.getBuildModel(buildId) ?: return
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val buildNoObj = triggerContainer.buildNo ?: return

        if (buildNoObj.buildNoType == BuildNoType.SUCCESS_BUILD_INCREMENT) {
            // 使用分布式锁防止并发更新
            val buildNoLock = PipelineBuildNoLock(redisOperation = redisOperation, pipelineId = pipelineId)
            try {
                buildNoLock.lock()
                updateBuildNoInfo(pipelineId, buildId)
            } finally {
                buildNoLock.unlock()
            }
        }
    }

    private fun updateBuildNoInfo(pipelineId: String, buildId: String) {
        val buildSummary = pipelineRuntimeService.getBuildSummaryRecord(pipelineId = pipelineId)
        val buildNo = buildSummary?.buildNo
        if (buildNo != null && pipelineRuntimeService.getBuildInfo(buildId)?.retryFlag != true) {
            pipelineRuntimeService.updateBuildNo(pipelineId = pipelineId, buildNo = buildNo + 1)
            // 更新历史表的推荐版本号
            val buildParameters = pipelineRuntimeService.getBuildParametersFromStartup(buildId)
            val recommendVersionPrefix = pipelineRuntimeService.getRecommendVersionPrefix(buildParameters)
            if (recommendVersionPrefix != null) {
                pipelineRuntimeService.updateRecommendVersion(
                    buildId = buildId,
                    recommendVersion = "$recommendVersionPrefix.$buildNo"
                )
            }
        }
    }

    private fun PipelineBuildFinishEvent.fixTask(buildInfo: BuildInfo) {
        val allBuildTask = pipelineTaskService.getAllBuildTask(buildId)
        val errorInfos = mutableListOf<ErrorInfo>()
        allBuildTask.forEach {
            // 将所有还在运行中的任务全部结束掉
            if (it.status.isRunning()) {
                // 构建机直接结束
                if (it.containerType == VMBuildContainer.classType) {
                    pipelineTaskService.updateTaskStatus(
                        task = it, userId = userId, buildStatus = BuildStatus.TERMINATE,
                        errorType = errorType, errorCode = errorCode, errorMsg = errorMsg
                    )
                } else {
                    pipelineEventDispatcher.dispatch(
                        PipelineBuildAtomTaskEvent(
                            source = javaClass.simpleName,
                            projectId = projectId, pipelineId = pipelineId, userId = it.starter,
                            stageId = it.stageId, buildId = it.buildId, containerId = it.containerId,
                            containerHashId = it.containerHashId, containerType = it.containerType,
                            taskId = it.taskId, taskParam = it.taskParams, actionType = ActionType.TERMINATE
                        )
                    )
                }
            }
            // 将插件出错信息逐一加入构建错误信息
            if (it.errorType != null) {
                errorInfos.add(ErrorInfo(
                    taskId = it.taskId,
                    taskName = it.taskName,
                    atomCode = it.atomCode ?: it.taskParams["atomCode"] as String? ?: it.taskType,
                    errorType = it.errorType?.num ?: ErrorType.USER.num,
                    errorCode = it.errorCode ?: PLUGIN_DEFAULT_ERROR,
                    errorMsg = CommonUtils.interceptStringInLength(
                        string = it.errorMsg, length = PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX) ?: ""
                ))
                // 做入库长度保护，假设超过上限则抛弃该错误信息
                if (JsonUtil.toJson(errorInfos).toByteArray().size > PIPELINE_MESSAGE_STRING_LENGTH_MAX) {
                    errorInfos.removeAt(errorInfos.lastIndex)
                }
            }
        }
        if (errorInfos.isNotEmpty()) buildInfo.errorInfoList = errorInfos
    }

    private fun PipelineBuildFinishEvent.popNextBuild() {

        // 获取下一个排队的
        val nextBuild = pipelineRuntimeExtService.popNextQueueBuildInfo(projectId = projectId, pipelineId = pipelineId)
        if (nextBuild == null) {
            LOG.info("ENGINE|$buildId|$source|FETCH_QUEUE|$pipelineId no queue build!")
            return
        }

        LOG.info("ENGINE|$buildId|$source|FETCH_QUEUE|next build: ${nextBuild.buildId} ${nextBuild.status}")
        val model = pipelineBuildDetailService.getBuildModel(nextBuild.buildId) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(nextBuild.buildId)
        )
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                source = "build_finish_$buildId",
                projectId = nextBuild.projectId,
                pipelineId = nextBuild.pipelineId,
                userId = nextBuild.startUser,
                buildId = nextBuild.buildId,
                taskId = nextBuild.firstTaskId,
                status = nextBuild.status,
                actionType = ActionType.START,
                buildNoType = triggerContainer.buildNo?.buildNoType
            )
        )
    }
}
