package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.ExecutionVariables
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

abstract class NotifyPipelineCmd @Autowired constructor(
    open val pipelineRepositoryService: PipelineRepositoryService,
    open val pipelineRuntimeService: PipelineRuntimeService,
    open val pipelineBuildFacadeService: PipelineBuildFacadeService,
    open val client: Client,
    open val buildVariableService: BuildVariableService
) : NotifyCmd {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val projectId = commandContext.projectId
        val pipelineId = commandContext.pipelineId
        val buildId = commandContext.buildId
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val pipelineName = pipelineInfo.pipelineName
        val executionVar = getExecutionVariables(
            pipelineId = pipelineId,
            vars = commandContext.variables as MutableMap<String, String>)
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: return
        val timeDuration = commandContext.variables[PIPELINE_TIME_DURATION]?.toLong() ?: 0L
        if (timeDuration > 0) {
            // 处理发送消息的耗时展示
            commandContext.variables[PIPELINE_TIME_DURATION] = DateTimeUtil.formatMillSecond(timeDuration * 1000)
        }

        val trigger = executionVar.trigger
        val buildNum = buildInfo.buildNum
        val user = executionVar.user
        val detail = pipelineBuildFacadeService.getBuildDetail(
            userId = buildInfo.startUser,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS,
            checkPermission = false
        )
        val failTask = getFailTaskName(detail)
        commandContext.notifyValue["failTask"] = failTask
        val projectName =
            client.get(ServiceProjectResource::class).get(projectId).data?.projectName.toString()
        val pipelineMap = mutableMapOf(
            "pipelineName" to pipelineName,
            "buildNum" to buildNum.toString(),
            "projectName" to projectName,
            "startTime" to getFormatTime(detail.startTime),
            "trigger" to trigger,
            "username" to user,
            "failTask" to failTask,
            "duration" to DateTimeUtil.formatMillSecond(timeDuration)
        )
        commandContext.notifyValue.putAll(pipelineMap)
    }

    private fun getFailTaskName(detail: ModelDetail): String {
        var result = "unknown"
        detail.model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.firstOrNull { "FAILED" == it.status }?.let {
                    result = it.name
                }
            }
        }
        return result
    }

    private fun getFormatTime(time: Long): String {
        val current = LocalDateTime.ofInstant(Date(time).toInstant(), ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    abstract fun getExecutionVariables(pipelineId: String, vars: MutableMap<String, String>): ExecutionVariables
}
