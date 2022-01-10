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
import com.tencent.devops.process.utils.PIPELINE_TIME_END
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
    override fun canExecute(commandContextBuild: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContextBuild: BuildNotifyContext) {
        val projectId = commandContextBuild.projectId
        val pipelineId = commandContextBuild.pipelineId
        val buildId = commandContextBuild.buildId
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val pipelineName = pipelineInfo.pipelineName
        val executionVar = getExecutionVariables(
            pipelineId = pipelineId,
            vars = commandContextBuild.variables as MutableMap<String, String>)
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: return
        val endTime = System.currentTimeMillis()
        val timeDuration = (endTime - buildInfo.startTime!!)
        commandContextBuild.variables[PIPELINE_TIME_DURATION] = DateTimeUtil.formatMillSecond(timeDuration)

        buildVariableService.setVariable(
            projectId = commandContextBuild.projectId,
            pipelineId = commandContextBuild.pipelineId,
            buildId = commandContextBuild.buildId,
            varName = PIPELINE_TIME_END,
            varValue = endTime
        )
        // 设置总耗时
        buildVariableService.setVariable(
            projectId = commandContextBuild.projectId,
            pipelineId = commandContextBuild.pipelineId,
            buildId = commandContextBuild.buildId,
            varName = PIPELINE_TIME_DURATION,
            varValue = timeDuration.toString()
        )

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
        commandContextBuild.notifyValue["failTask"] = failTask
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
        commandContextBuild.notifyValue.putAll(pipelineMap)
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
