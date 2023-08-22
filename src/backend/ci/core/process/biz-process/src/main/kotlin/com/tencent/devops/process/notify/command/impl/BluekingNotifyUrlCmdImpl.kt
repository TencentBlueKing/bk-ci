package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.notify.command.BuildNotifyContext
import org.springframework.beans.factory.annotation.Autowired

class BluekingNotifyUrlCmdImpl @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService
) : NotifyUrlBuildCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val projectId = commandContext.projectId
        val pipelineId = commandContext.pipelineId
        val buildId = commandContext.buildId
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val pipelineName = pipelineInfo.pipelineName

        // 判断codecc类型更改查看详情链接
        val detailUrl = if (pipelineInfo.channelCode == ChannelCode.CODECC) {
            "${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/task/$pipelineName/detail"
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

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${HomeHostUtil.outerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"
}
