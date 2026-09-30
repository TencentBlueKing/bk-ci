package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.engine.service.PipelineCacheService

class DefaultStatusPageBuild(
    private val pipelineCacheService: PipelineCacheService
) : StatusPageBuild() {

    override fun getChannel(buildPageInfo: BuildPageInfo): ChannelCode =
        pipelineCacheService.getChannelCode(buildPageInfo.projectId, buildPageInfo.pipelineId)
            ?: ChannelCode.getRequestChannelCode()

    override fun extStatusPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
