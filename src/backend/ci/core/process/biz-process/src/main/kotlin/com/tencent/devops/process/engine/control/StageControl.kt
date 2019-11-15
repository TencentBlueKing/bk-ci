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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 *  步骤控制器
 * @version 1.0
 */
@Service
class StageControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun handle(event: PipelineBuildStageEvent): Boolean {
        with(event) {

            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
                ?: run {
                    logger.info("[$buildId]|STAGE_$actionType| have not build detail!")
                    return true
                }

            // 当前构建整体的状态，可能是运行中，也可能已经失败
            // 已经结束的构建，不再受理，抛弃消息
            if (BuildStatus.isFinish(buildInfo.status)) {
                logger.info("[$buildId]|[${buildInfo.status}]|STAGE_REPEAT_EVENT|event=$event")
                return true
            }

            val stages = pipelineRuntimeService.listStages(buildId)

            val allContainers = pipelineRuntimeService.listContainers(buildId)

            var buildStatus: BuildStatus = BuildStatus.SUCCEED

            // 终止命令 不需要判断各个Stage的状态，可直接停止
            if (ActionType.isTerminate(actionType)) {
                logger.info("[$buildId]|[${buildInfo.status}]|STAGE_TERMINATE|stageId=$stageId")
                stages.forEach { stage ->
                    pipelineRuntimeService.updateStageStatus(buildId, stage.stageId, BuildStatus.TERMINATE)
                }
                allContainers.forEach { c ->
                    if (BuildStatus.isRunning(c.status))
                        pipelineRuntimeService.updateContainerStatus(
                            buildId = buildId,
                            stageId = c.stageId,
                            containerId = c.containerId,
                            endTime = LocalDateTime.now(),
                            buildStatus = BuildStatus.TERMINATE
                        )
                }
                return sendTerminateEvent(BuildStatus.FAILED)
            }

            var nextStage = false
//            var send = false
            run outer@{
                stages.forEach { stage ->
                    if (stageId == stage.stageId || nextStage) {
                        // 执行成功则结束本次事件处理，否则要尝试下一stage
                        buildStatus = stage.judgeStageContainer(allContainers, actionType, userId)

                        logger.info("[$buildId]|[${buildInfo.status}]|STAGE_DONE|stageId=${stage.stageId}|status=$buildStatus|action=$actionType")

                        // 如果当前Stage[还未结束]或者[执行失败]，则不尝试下一Stage
                        if (BuildStatus.isRunning(buildStatus) || BuildStatus.isFailure(buildStatus)) {
                            return@outer // 不尝试下一Stage
                        } else {
                            nextStage = true // 表示要执行下一个Stage
                        }
                    }
                }
            }
            if (BuildStatus.isFinish(buildStatus)) { // 构建状态结束了
                logger.info("[$buildId]|[${buildInfo.status}]|STAGE_FINALLY|stageId=$stageId|status=$buildStatus|action=$actionType")
                sendFinishEvent("FINALLY", buildStatus)
            }
        }
        return true
    }

    /**
     * 判断当前Stage具备下发消息的场景并下发Container消息
     * @param allContainers 所有Job对象
     * @param actionType 事件指令
     * @param userId 执行者
     * @return 返回true表示已经下发，如下发失败则返回false
     */
    private fun PipelineBuildStage.judgeStageContainer(
        allContainers: Collection<PipelineBuildContainer>,
        actionType: ActionType,
        userId: String
    ): BuildStatus {

        var buildStatus = status
        var newActionType = actionType
        // 针对刚启动的Stage
        if (BuildStatus.isReadyToRun(status)) {
            if (actionType == ActionType.REFRESH) { // 对未启动的Stage要变成开始指令
                newActionType = ActionType.START
            }
            // 要启动Stage，初始化状态
            if (ActionType.isStart(newActionType)) {
                buildStatus = BuildStatus.RUNNING
                pipelineRuntimeService.updateStageStatus(buildId, stageId, buildStatus)
                logger.info("[$buildId]|STAGE_INIT|stageId=$stageId|action=$newActionType")
            }
            // 要终止或者跳过的Stage，直接设置为取消
            else if (ActionType.isEnd(newActionType) || actionType == ActionType.SKIP) {
                buildStatus = BuildStatus.CANCELED
                val now = LocalDateTime.now()
                pipelineRuntimeService.updateStage(
                    buildId = buildId,
                    stageId = stageId,
                    startTime = now,
                    endTime = now,
                    buildStatus = buildStatus
                )
                logger.info("[$buildId]|STAGE_$actionType|stageId=$stageId|action=$newActionType")
                return buildStatus
            }
        }

        var finishContainers = 0
        var failureContainers = 0
        val containers = mutableListOf<PipelineBuildContainer>()
        allContainers.forEach { c ->
            if (c.stageId == stageId) {
                containers.add(c)
                if (BuildStatus.isFinish(c.status)) {
                    finishContainers++
                }
                if (BuildStatus.isFailure(c.status)) {
                    failureContainers++
                }
            }
        }

        if (finishContainers < containers.size) { // 还有未执行完的任务,继续下发其他构建容器
            sendContainerEvent(containers, newActionType, userId)
        } else if (finishContainers == containers.size) { // 全部执行完的
            buildStatus = if (failureContainers == 0) {
                BuildStatus.SUCCEED
            } else {
                BuildStatus.FAILED
            }
            pipelineRuntimeService.updateStageStatus(buildId, stageId, buildStatus)
            logger.info("[$buildId]|STAGE_CONTAINER_FINISH|stageId=$stageId|status=$buildStatus|action=$actionType")
        }
        return buildStatus
    }

    private fun PipelineBuildStageEvent.sendTerminateEvent(buildStatus: BuildStatus): Boolean {
        pipelineEventDispatcher.dispatch(
            PipelineBuildCancelEvent(
                source = javaClass.simpleName,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = buildStatus
            )
        )
        return true
    }

    private fun PipelineBuildStageEvent.sendFinishEvent(source: String, buildStatus: BuildStatus) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = buildStatus
            )
        )
    }

    /**
     * 下发容器构建事件
     * @param containers 当前container对象列表
     * @param actionType 事件指令
     * @param userId 执行者
     * @return 返回true表示已经下发，如下发失败则返回false
     */
    private fun PipelineBuildStage.sendContainerEvent(
        containers: Collection<PipelineBuildContainer>,
        actionType: ActionType,
        userId: String
    ) {
        // 同一Stage下的多个Container是并行
        containers.forEach nextContainer@{ container ->
            val containerStatus = container.status
            val containerId = container.containerId
            logger.info("[$buildId]|stage=$stageId|container=$containerId|status=$containerStatus")
            if (BuildStatus.isFinish(containerStatus)) {
                return@nextContainer // 已经执行完毕的直接跳过
            } else if (BuildStatus.isReadyToRun(containerStatus) && !ActionType.isStart(actionType)) {
                return@nextContainer // 失败或可重试的容器，如果不是重试动作，则跳过
            } else if (BuildStatus.isRunning(containerStatus) && !ActionType.isTerminate(actionType)) {
                return@nextContainer // 已经在运行中的, 只接受强制终止
            }

            container.sendBuildContainerEvent(actionType, userId)
            logger.info("[$buildId]|STAGE_CONTAINER_$actionType|stage=$stageId|container=$containerId")
        }
    }

    private fun PipelineBuildContainer.sendBuildContainerEvent(actionType: ActionType, userId: String) {
        // 通知容器构建消息
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "start_container",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = stageId,
                containerType = containerType,
                containerId = containerId,
                actionType = actionType
            )
        )
    }
}
