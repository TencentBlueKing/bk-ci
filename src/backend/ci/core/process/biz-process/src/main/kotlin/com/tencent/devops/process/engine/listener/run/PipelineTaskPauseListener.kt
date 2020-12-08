package com.tencent.devops.process.engine.listener.run

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.listener.pipeline.BaseListener
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.process.engine.common.BS_MANUAL_STOP_PAUSE_ATOM
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelinePauseValueDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineTaskPauseEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineWebsocketService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildVariableService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
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
    val pipelineBuildTaskDao: PipelineBuildTaskDao,
    val pipelineRuntimeService: PipelineRuntimeService,
    val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter,
    val pipelinePauseValueDao: PipelinePauseValueDao
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
                    taskName = taskRecord!!.taskName
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
        taskName: String
    ) {
        continuePauseTask(
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            stageId = stageId,
            containerId = containerId
        )

        val params = mutableListOf<BuildParameters>()
        buildVariableService.batchSetVariable(dslContext, projectId, pipelineId, buildId, params)

        val newElementRecord = pipelinePauseValueDao.get(dslContext, buildId, taskId)
        if (newElementRecord != null) {
            val newElement = JsonUtil.to(newElementRecord.newValue, Element::class.java)
            // 修改插件运行设置
            pipelineBuildTaskDao.updateTaskParam(dslContext, buildId, taskId, objectMapper.writeValueAsString(newElement))
            logger.info("update task param success | $buildId| $taskId ")

            // 修改详情model
            buildDetailService.updateElementWhenPauseContinue(buildId, stageId, containerId, taskId, newElement)
        } else {
            buildDetailService.updateElementWhenPauseContinue(buildId, stageId, containerId, taskId, null)
        }

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
        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = "[$taskName] processed. user:$userId, action: continue",
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

        // 刷新detail内model
        buildDetailService.taskCancel(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId
        )

        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = "[$taskName] processed . user:$userId, action: terminate",
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
                source = BS_MANUAL_STOP_PAUSE_ATOM,
                actionType = ActionType.END,
                pipelineId = pipelineId,
                projectId = projectId,
                userId = userId,
                buildId = buildId,
                containerId = containerId,
                stageId = stageId,
                containerType = containerRecord?.containerType ?: "vmBuild"
            )
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
                } else if (task.taskName.startsWith(VMUtils.getWaitLable()) && task.taskId.startsWith(VMUtils.getEndLable())) {
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
