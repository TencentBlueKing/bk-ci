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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.TaskAtomService
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.common.BS_TASK_HOST
import com.tencent.devops.process.engine.control.lock.TaskIdLock
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.PipelineTaskService
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
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handle(event: PipelineBuildAtomTaskEvent): Boolean {
        with(event) {
            val taskIdLock = TaskIdLock(redisOperation, buildId, taskId)
            try {
                taskIdLock.lock()
                execute()
            } finally {
                taskIdLock.unlock()
            }
        }
        return true
    }

    private fun PipelineBuildAtomTaskEvent.execute() {

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)

        val buildTask = pipelineRuntimeService.getBuildTask(buildId, taskId)
        // 检查构建状态,防止重复跑
        if (buildInfo == null || BuildStatus.isFinish(buildInfo.status) || buildTask == null || BuildStatus.isFinish(
                buildTask.status
            )
        ) {
            logger.warn("[$buildId]|ATOM_$actionType|taskId=$taskId| status=${buildTask?.status ?: "not exists"}")
            return
        }

        // 构建机的任务不在此运行
        if (taskAtomService.runByVmTask(buildTask)) {
            logger.info("[$buildId]|ATOM|stageId=$stageId|container=$containerId|taskId=$taskId|vm atom will claim by agent")
            return
        }

        buildTask.starter = userId

        var delayMillsNext = delayMills

        if (taskParam.isNotEmpty()) { // 追加事件传递的参数变量值
            buildTask.taskParams.putAll(taskParam)
        }

        logger.info("[$buildId]|[${buildInfo.status}]|ATOM_$actionType|taskId=$taskId|status=${buildTask.status}")

        val buildStatus = when {
            BuildStatus.isReadyToRun(buildTask.status) -> { // 准备启动执行
                if (ActionType.isEnd(actionType)) { // #2400 因任务终止&结束的事件命令而未执行的原子设置为UNEXEC，而不是SKIP
                    pipelineRuntimeService.updateTaskStatus(buildId, taskId, userId, BuildStatus.UNEXEC)

                    BuildStatus.UNEXEC // SKIP 仅当是用户意愿明确正常运行情况要跳过执行的，不影响主流程的才能是SKIP
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
            // 如果是要轮循的才需要定时消息轮循
            val loopDelayMills =
                if (buildTask.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] != null) {
                    buildTask.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS].toString().trim().toInt()
                } else {
                    5000
                }
            // 将执行结果参数写回事件消息中，方便再次传递
            taskParam.putAll(buildTask.taskParams)
            delayMills = loopDelayMills
            actionType = ActionType.REFRESH // 尝试刷新任务状态
            // 特定消费者
            if (buildTask.taskParams[BS_TASK_HOST] != null) {
                routeKeySuffix = buildTask.taskParams[BS_TASK_HOST].toString()
            }
            pipelineEventDispatcher.dispatch(this)
        } else {
            val nextActionType = if (BuildStatus.isFailure(buildStatus)) {
                // 如果配置了失败重试，且重试次数上线未达上限，则进行重试
                if (pipelineTaskService.isRetryWhenFail(taskId, buildId)) {
                    logger.info("retry task [$buildId]|ATOM|stageId=$stageId|container=$containerId|taskId=$taskId |vm atom will retry, even the task is failure")
                    pipelineRuntimeService.updateTaskStatus(buildId, taskId, userId, BuildStatus.RETRY)
                    delayMillsNext = 5000
                    ActionType.RETRY
                } else if (ControlUtils.continueWhenFailure(buildTask.additionalOptions)) { // 如果配置了失败继续，则继续下去
                    logger.info("[$buildId]|ATOM|stageId=$stageId|container=$containerId|taskId=$taskId|vm atom will continue, even the task is failure")
                    // 记录失败原子
                    pipelineTaskService.createFailElementVar(
                        buildId = buildId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        taskId = taskId
                    )

                    if (ActionType.isEnd(actionType)) ActionType.START
                    else actionType
                } else { // 如果当前动作不是结束动作并且当前状态失败了就要结束当前容器构建
                    // 记录失败原子
                    pipelineTaskService.createFailElementVar(
                        buildId = buildId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        taskId = taskId
                    )
                    if (!ActionType.isEnd(actionType)) ActionType.END
                    else actionType // 如果是结束动作，继承它
                }
            } else {
                // 清除该原子内的重试记录
                pipelineTaskService.removeRetryCache(buildId, taskId)
                // 清理插件错误信息（重试插件成功的情况下）
                pipelineTaskService.removeFailVarWhenSuccess(
                    buildId = buildId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = taskId
                )
                // 当前原子成功结束后，继续继承动作，发消息请求执行
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
                    actionType = nextActionType,
                    delayMills = delayMillsNext
                )
            )
        }
    }

    private fun atomBuildStatus(response: AtomResponse): BuildStatus {
        return response.buildStatus
    }
}
