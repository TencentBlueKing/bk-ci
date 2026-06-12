package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.engine.service.PipelineChannelCacheService

class DefaultStatusPageBuild(
    private val pipelineChannelCacheService: PipelineChannelCacheService
) : StatusPageBuild() {

    override fun getChannel(buildPageInfo: BuildPageInfo): ChannelCode =
        pipelineChannelCacheService.getChannelCode(buildPageInfo.projectId, buildPageInfo.pipelineId)
            ?: ChannelCode.getRequestChannelCode()

    override fun extStatusPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
