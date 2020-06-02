package com.tencent.devops.process.engine.listener.run

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineTaskPauseEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineWebsocketService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildVariableService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PipelineTaskPauseListener @Autowired constructor(
    pipelineEventDispatcher: PipelineEventDispatcher,
    val webSocketDispatcher: WebSocketDispatcher,
    val redisOperation: RedisOperation,
    val websocketService: PipelineWebsocketService,
    val buildDetailService: PipelineBuildDetailService,
    val dslContext: DSLContext,
    val buildVariableService: BuildVariableService,
    val rabbitTemplate: RabbitTemplate,
    val pipelineBuildTaskDao: PipelineBuildTaskDao,
    val pipelineRuntimeService: PipelineRuntimeService,
    val objectMapper: ObjectMapper
) : BaseListener<PipelineTaskPauseEvent>(pipelineEventDispatcher) {
    override fun run(event: PipelineTaskPauseEvent) {
        val taskRecord = pipelineRuntimeService.getBuildTask(event.buildId, event.taskId)
        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = event.buildId)
        try {
            redisLock.lock()
            if (event.actionType == ActionType.REFRESH) {
                taskContinue(
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    taskId = event.taskId,
                    stageId = event.stageId,
                    containerId = event.containerId,
                    projectId = event.projectId,
                    userId = event.userId,
                    taskName = taskRecord!!.taskName,
                    element = event.element
                )
            } else if (event.actionType == ActionType.TERMINATE) {
                taskCancel(
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    taskId = event.taskId,
                    stageId = event.stageId,
                    containerId = event.containerId,
                    projectId = event.projectId,
                    userId = event.userId,
                    taskName = taskRecord!!.taskName
                )
            }
            webSocketDispatcher.dispatch(
                websocketService.buildDetailMessage(
                    buildId = event.buildId,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    userId = event.userId
                )
            )
        } catch (e: Exception) {
            logger.warn("pause task execute fail,$e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun taskContinue(
        pipelineId: String,
        buildId: String,
        taskId: String,
        stageId: String,
        containerId: String,
        projectId: String,
        userId: String,
        taskName: String,
        element: Element
    ) {
        continuePauseTask(
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            stageId = stageId,
            containerId = containerId
        )

        val params = mutableMapOf<String, Any>()
        buildVariableService.batchSetVariable(projectId, pipelineId, buildId, params)
        // 修改插件运行设置
        pipelineBuildTaskDao.updateTaskParam(dslContext, buildId, taskId, objectMapper.writeValueAsString(element))
        logger.info("update task param success | $buildId| $taskId | $element")

        // 修改详情model
        buildDetailService.updateElementWhenPauseContinue(buildId, stageId, containerId, taskId, element)
        logger.info("update detail element success | $buildId| $taskId | $element")

        // 触发引擎container事件，继续后续流程
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "pauseContinue",
                containerId = containerId,
                stageId = stageId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = userId,
                projectId = projectId,
                actionType = ActionType.REFRESH,
                containerType = ""
            )
        )
        LogUtils.addYellowLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "【$taskName】已完成人工处理。处理人:$userId, 操作：继续",
            tag = taskId,
            jobId = containerId,
            executeCount = 1
        )
    }

    private fun taskCancel(
        pipelineId: String,
        buildId: String,
        taskId: String,
        stageId: String,
        containerId: String,
        projectId: String,
        userId: String,
        taskName: String
    ) {
        logger.info("task cancel|$projectId| $pipelineId| $buildId| $stageId| $containerId| $taskId")
        // 修改插件状态位运行
        pipelineRuntimeService.updateTaskStatus(
            buildId = buildId,
            taskId = taskId,
            userId = "",
            buildStatus = BuildStatus.CANCELED
        )
        logger.info("taskCancel update|$buildId|$taskId| task status  to ${BuildStatus.CANCELED}")

        // 修改容器状态位运行
        pipelineRuntimeService.updateContainerStatus(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            startTime = null,
            endTime = null,
            buildStatus = BuildStatus.CANCELED
        )
        logger.info("taskCancel update|$buildId|$taskId| container status  to ${BuildStatus.CANCELED}")

        // 刷新detail内model
        buildDetailService.taskCancel(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId
        )

        LogUtils.addYellowLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "【$taskName】已完成人工处理。处理人:$userId, 操作：停止",
            tag = taskId,
            jobId = containerId,
            executeCount = 1
        )
        val containerRecord = pipelineRuntimeService.getContainer(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId
        )

        // 刷新stage状态
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "taskCancel",
                actionType = ActionType.END,
                pipelineId = pipelineId,
                projectId = projectId,
                userId = userId,
                buildId = buildId,
                containerId = containerId,
                stageId = stageId,
                containerType = containerRecord?.containerType ?: "vmBuild"
            )
//            PipelineBuildStageEvent(
//                source = "taskCancel",
//                stageId = stageId,
//                pipelineId = pipelineId,
//                projectId = projectId,
//                actionType = ActionType.REFRESH,
//                buildId = buildId,
//                userId = userId
//            )
        )
    }

    private fun continuePauseTask(
        pipelineId: String,
        buildId: String,
        taskId: String,
        stageId: String,
        containerId: String
    ) {
        logger.info("executePauseBuild pipelineId[$pipelineId], buildId[$buildId] stageId[$stageId] containerId[$containerId] taskId[$taskId]")
//        // 修改任务状态位运行
//        pipelineRuntimeService.updateTaskStatus(
//            buildId = buildId,
//            taskId = taskId,
//            userId = "",
//            buildStatus = BuildStatus.QUEUE
//        )
//        logger.info("update|$buildId|$taskId| task status  to ${BuildStatus.QUEUE}")

        // 将启动和结束任务置为排队。用于启动构建机
        val taskRecords = pipelineRuntimeService.getAllBuildTask(buildId)
        val startAndEndTask = mutableListOf<PipelineBuildTask>()
        taskRecords.forEach { task ->
            if (task.containerId == containerId && task.stageId == stageId) {
                if (task.taskId == taskId) {
                    startAndEndTask.add(task)
                } else if (task.taskName.startsWith(VMUtils.getCleanVmLable()) && task.taskId.startsWith(VMUtils.getStopVmLabel())) {
                    startAndEndTask.add(task)
                } else if (task.taskName.startsWith(VMUtils.getPrepareVmLable()) && task.taskId.startsWith(VMUtils.getStartVmLabel())) {
                    startAndEndTask.add(task)
                }
            }
        }
        startAndEndTask.forEach {
            pipelineRuntimeService.updateTaskStatus(
                buildId = buildId,
                taskId = it.taskId,
                userId = "",
                buildStatus = BuildStatus.QUEUE
            )
            logger.info("update|$buildId|${it.taskId}| task status from ${it.status} to ${BuildStatus.QUEUE}")
        }

        // 修改容器状态位运行
        pipelineRuntimeService.updateContainerStatus(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            startTime = null,
            endTime = null,
            buildStatus = BuildStatus.QUEUE
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}