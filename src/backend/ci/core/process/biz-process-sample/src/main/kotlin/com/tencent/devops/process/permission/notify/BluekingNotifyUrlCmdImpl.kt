package com.tencent.devops.process.permission.notify

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.impl.NotifyUrlBuildCmd
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.springframework.beans.factory.annotation.Autowired

class BluekingNotifyUrlCmdImpl @Autowired constructor(
    val pipelineRepositoryService: PipelineRepositoryService,
    val pipelineRuntimeService: PipelineRuntimeService,
    val pipelineBuildFacadeService: PipelineBuildFacadeService
) : NotifyUrlBuildCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val projectId = commandContext.projectId
        val pipelineId = commandContext.pipelineId
        val buildId = commandContext.buildId
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        var pipelineName = pipelineInfo.pipelineName
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: return

        // 判断codecc类型更改查看详情链接
        val detailUrl = if (pipelineInfo.channelCode == ChannelCode.CODECC) {
            val detail = pipelineBuildFacadeService.getBuildDetail(userId = buildInfo.startUser,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS,
                checkPermission = false)
            val codeccModel = getCodeccTaskName(detail)
            if (codeccModel != null) {
                pipelineName = codeccModel.codeCCTaskName.toString()
            }
            val taskId = pipelineName
            "${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/task/$taskId/detail"
        } else {
            detailUrl(projectId, pipelineId, buildId)
        }
        val urlMap = mutableMapOf(
            "detailUrl" to detailUrl,
            "detailOuterUrl" to detailUrl,
            "detailShortOuterUrl" to detailUrl
        )
        commandContext.notifyValue.putAll(urlMap)
    }

    @Suppress("NestedBlockDepth")
    private fun getCodeccTaskName(detail: ModelDetail): LinuxCodeCCScriptElement? {
        for (stage in detail.model.stages) {
            stage.containers.forEach { container ->
                var codeccElemet =
                    container.elements.filter { it is LinuxCodeCCScriptElement || it is LinuxPaasCodeCCScriptElement }
                if (codeccElemet.isNotEmpty()) return codeccElemet.first() as LinuxCodeCCScriptElement
                container.fetchGroupContainers()?.forEach {
                    codeccElemet =
                        it.elements.filter { it is LinuxCodeCCScriptElement || it is LinuxPaasCodeCCScriptElement }
                    if (codeccElemet.isNotEmpty()) return codeccElemet.first() as LinuxCodeCCScriptElement
                }
            }
        }
        return null
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${HomeHostUtil.outerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"
}
