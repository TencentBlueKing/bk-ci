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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.TaskAtomService
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.common.Loop
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 任务（最小单元Atom）控制器
 * @version 1.0
 */
@Service
class TaskControl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val taskAtomService: TaskAtomService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StageControl::class.java)!!
        private const val ExpiredTimeInSeconds: Long = 60
    }

    fun handle(event: PipelineBuildAtomTaskEvent) {
        with(event) {
            val redisLock = RedisLock(redisOperation, "lock.build.$buildId.t_$taskId", ExpiredTimeInSeconds)
            try {
                redisLock.lock()
                return execute()
            } finally {
                redisLock.unlock()
            }
        }
    }

    private fun PipelineBuildAtomTaskEvent.execute() {
        val buildTask = pickTask() ?: return

        buildTask.starter = userId

        if (taskParam.isNotEmpty()) { // 追加事件传递的参数变量值
            buildTask.taskParams.putAll(taskParam)
        }

        logger.info("[$buildId]|ATOM_$actionType|taskId=$taskId|status=${buildTask.status}")
        val buildStatus = when {
            BuildStatus.isReadyToRun(buildTask.status) -> { // 准备启动执行
                if (ActionType.isEnd(actionType)) {
                    pipelineRuntimeService.updateTaskStatus(buildId, taskId, userId, BuildStatus.SKIP)
                    BuildStatus.SKIP // 未执行的插件设置为SKIP或UNEXEC
                } else {
                    atomBuildStatus(taskAtomService.start(buildTask))
                }
            }
            BuildStatus.isRunning(buildTask.status) -> { // 运行中的，检查是否运行结束，以及决定是否强制终止
                atomBuildStatus(taskAtomService.tryFinish(buildTask, ActionType.isTerminate(actionType)))
            }
            else -> buildTask.status // 其他状态不做动作
        }

        if (BuildStatus.isRunning(buildStatus)) { // 仍然在运行中--没有结束的
            dispatchNextTime(buildTask)
        } else {
            sendBackToContainer(buildStatus, buildTask)
        }
    }

    private fun PipelineBuildAtomTaskEvent.sendBackToContainer(buildStatus: BuildStatus, buildTask: PipelineBuildTask) {
        val nextActionType = if (BuildStatus.isFailure(buildStatus)) {
            // 如果配置了失败继续，则继续下去
            if (ControlUtils.continueWhenFailure(buildTask.additionalOptions)) {
                logger.info("[$buildId]|FAIL_CONTINUE_ATOM|stageId=$stageId|container=$containerId|taskId=$taskId")
                if (ActionType.isEnd(actionType)) ActionType.START
                else actionType
            } else { // 如果当前动作不是结束动作并且当前状态失败了就要结束当前容器构建
                if (!ActionType.isEnd(actionType)) ActionType.END
                else actionType // 如果是结束动作，继承它
            }
        } else {
            // 当前插件成功结束后，继续继承动作，发消息请求执行
            actionType
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "taskControl_$buildStatus",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                containerType = containerType,
                actionType = nextActionType
            )
        )
    }

    private fun PipelineBuildAtomTaskEvent.dispatchNextTime(buildTask: PipelineBuildTask) {
        // 如果是要轮循的才需要定时消息轮循
        val delayMills =
            if (buildTask.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] != null) {
                buildTask.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS].toString().trim().toInt()
            } else {
                Loop.DefaultIntervalMills
            }
        // 将执行结果参数写回事件消息中，方便再次传递
        this.taskParam.putAll(buildTask.taskParams)
        this.delayMills = delayMills
        this.actionType = ActionType.REFRESH // 尝试刷新任务状态
        // 特定消费者
        if (buildTask.taskParams[BS_TASK_HOST] != null) {
            this.routeKeySuffix = buildTask.taskParams[BS_TASK_HOST].toString()
        }
        pipelineEventDispatcher.dispatch(this)
    }

    private fun PipelineBuildAtomTaskEvent.pickTask(): PipelineBuildTask? {

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)

        val buildTask = pipelineRuntimeService.getBuildTask(buildId, taskId)
        // 检查构建状态,防止重复跑
        if (buildInfo == null || BuildStatus.isFinish(buildInfo.status) || buildTask == null) {
            logger.warn("[$buildId]|ATOM_$actionType|taskId=$taskId| status=${buildInfo?.status ?: "not exists"}")
            return null
        }

        // 构建机的任务不在此运行
        return if (taskAtomService.runByVmTask(buildTask)) {
            logger.info("[$buildId]|VM_ATOM|stageId=$stageId|container=$containerId|taskId=$taskId")
            null
        } else buildTask
    }

    private fun atomBuildStatus(response: AtomResponse): BuildStatus {
        return response.buildStatus
    }
}
