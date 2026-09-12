package com.tencent.devops.process.websocket.page

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.process.engine.service.PipelineCacheService

class DefaultRecordPageBuild(
    private val pipelineCacheService: PipelineCacheService
) : RecordPageBuild() {

    override fun getChannel(buildPageInfo: BuildPageInfo): ChannelCode =
        pipelineCacheService.getChannelCode(buildPageInfo.projectId, buildPageInfo.pipelineId)
            ?: ChannelCode.getRequestChannelCode()

    override fun extRecordPage(buildPageInfo: BuildPageInfo): String? {
        return null
    }
}
