package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.notify.command.BuildNotifyContext
import org.springframework.beans.factory.annotation.Autowired

class BluekingNotifyUrlCmdImpl @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService
) : NotifyUrlBuildCmd() {

    companion object {
        // 创作流渠道 URL 前缀
        private const val CREATIVE_STREAM_PREFIX = "creative-stream"
        // 流水线渠道 URL 前缀
        private const val PIPELINE_PREFIX = "pipeline"
    }

    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val projectId = commandContext.projectId
        val pipelineId = commandContext.pipelineId
        val buildId = commandContext.buildId
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val pipelineName = pipelineInfo.pipelineName
        val channelCode = pipelineInfo.channelCode

        // 判断codecc类型更改查看详情链接
        val detailUrl = if (channelCode == ChannelCode.CODECC) {
            "${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/task/$pipelineName/detail"
        } else {
            detailUrl(projectId, pipelineId, buildId, channelCode)
        }
        val urlMap = mutableMapOf(
            "detailUrl" to detailUrl,
            "detailOuterUrl" to detailUrl,
            "detailShortOuterUrl" to detailUrl
        )
        commandContext.notifyValue.putAll(urlMap)
    }

    /**
     * 根据渠道生成构建详情 URL
     * 流水线渠道: /console/pipeline/{projectId}/{pipelineId}/detail/{buildId}
     * 创作流渠道: /console/creative-stream/{projectId}/flow/{pipelineId}/execute/{buildId}/execute-detail
     */
    private fun detailUrl(
        projectId: String,
        pipelineId: String,
        processInstanceId: String,
        channelCode: ChannelCode
    ): String {
        val host = HomeHostUtil.outerServerHost()
        return if (channelCode == ChannelCode.CREATIVE_STREAM) {
            "$host/console/$CREATIVE_STREAM_PREFIX/$projectId/flow/$pipelineId" +
                "/execute/$processInstanceId/execute-detail"
        } else {
            "$host/console/$PIPELINE_PREFIX/$projectId/$pipelineId/detail/$processInstanceId"
        }
    }
}
