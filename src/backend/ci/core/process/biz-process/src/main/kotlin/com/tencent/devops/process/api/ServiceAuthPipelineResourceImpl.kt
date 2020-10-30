package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceAuthPipelineResource
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAuthPipelineResourceImpl @Autowired constructor(
    private val pipelineService: PipelineService
) : ServiceAuthPipelineResource {

    override fun pipelineList(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?
    ): Result<PipelineViewPipelinePage<PipelineInfo>> {
        return Result(pipelineService.getPipeline(projectId, page, pageSize))
    }

    override fun pipelineInfos(pipelineIds: Set<String>): Result<List<SimplePipeline>?> {
        return Result(pipelineService.getPipelineByIds(pipelineIds))
    }
}