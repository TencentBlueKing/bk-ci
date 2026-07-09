package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.engine.service.PipelineChannelCacheService

class DefaultHistoryPageBuild(
    private val pipelineChannelCacheService: PipelineChannelCacheService
) : HistoryPageBuild() {

    override fun getChannel(buildPageInfo: BuildPageInfo): ChannelCode =
        pipelineChannelCacheService.getChannelCode(buildPageInfo.projectId, buildPageInfo.pipelineId)
            ?: ChannelCode.getRequestChannelCode()

    override fun extHistoryPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
