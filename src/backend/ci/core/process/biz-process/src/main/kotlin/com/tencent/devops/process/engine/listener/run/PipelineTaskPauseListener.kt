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
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
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
    val pipelineBuildTaskDao: PipelineBuildTaskDao,
    val pipelineRuntimeService: PipelineRuntimeService,
    val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter
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

        findDiffValue(element, buildId, taskId, userId)

        val params = mutableListOf<BuildParameters>()
        buildVariableService.batchSetVariable(dslContext, projectId, pipelineId, buildId, params)
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
        buildLogPrinter.addYellowLine(
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

        // 刷新detail内model
        buildDetailService.taskCancel(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId
        )

        buildLogPrinter.addYellowLine(
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

    fun findDiffValue(newElement: Element, buildId: String, taskId: String, userId: String) {
        logger.info("start find diff new element|${objectMapper.writeValueAsString(newElement)}")
        val newJson = JsonUtil.toMap(newElement)
        val data = newJson["data"]
        val newInput = JsonUtil.toMap(data!!)["input"]
        val newInputData = newInput?.let { JsonUtil.toMap(it) }
        val inputKeys = newInputData?.keys ?: mutableSetOf()
        logger.info("inputKeys $inputKeys")
        val oldElement = pipelineRuntimeService.getBuildTask(buildId, taskId)
        logger.info(
            "end pause task new element|${objectMapper.writeValueAsString(newElement)}| oldElement|${objectMapper.writeValueAsString(
                oldElement
            )}"
        )
        val oldJson = oldElement?.taskParams
        val oldData = oldJson?.get("data")
        val oldInput = JsonUtil.toMap(oldData!!)["input"]
        val oldInputData = oldInput?.let { JsonUtil.toMap(it) }
        inputKeys.forEach {
            logger.info("continue pause task, key[$it] oldInput:${oldInputData?.get(it)}, newInput:${newInputData?.get(it)}")
            if (oldInputData != null && newInputData != null) {
                if (oldInputData!![it] != (newInputData!![it])) {
                    logger.info("input update, add Log, key $it, newData ${newInputData!![it]}, oldData ${oldInputData!![it]}")
                    buildLogPrinter.addYellowLine(
                        buildId = buildId,
                        message = "当前插件${oldElement.taskName}执行参数 $it 已变更",
                        tag = taskId,
                        jobId = VMUtils.genStartVMTaskId(oldElement.containerId),
                        executeCount = 1
                    )
                    buildLogPrinter.addYellowLine(
                        buildId = buildId,
                        message = "变更前：${oldInputData[it]}",
                        tag = taskId,
                        jobId = VMUtils.genStartVMTaskId(oldElement.containerId),
                        executeCount = 1
                    )
                    buildLogPrinter.addYellowLine(
                        buildId = buildId,
                        message = "变更后：${newInputData[it]}",
                        tag = taskId,
                        jobId = VMUtils.genStartVMTaskId(oldElement.containerId),
                        executeCount = 1
                    )
                }
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}