package com.tencent.devops.process.api.quality

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.quality.pojo.PipelineListRequest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.quality.QualityPipeline
import com.tencent.devops.process.service.TXPipelineService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityPipelineResourceImpl @Autowired constructor(
    private val txPipelineService: TXPipelineService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : UserQualityPipelineResource {
    override fun getPipelineInfo(userId: String, projectId: String, pipelineId: String, channelCode: ChannelCode?): Result<PipelineInfo> {
        return Result(pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode) ?: throw RuntimeException("pipeline info not found for $pipelineId"))
    }

    override fun listViewPipelines(userId: String, projectId: String, keywords: String?, page: Int?, pageSize: Int?, viewId: String?): Result<PipelineViewPipelinePage<QualityPipeline>> {
        return Result(txPipelineService.listQualityViewPipelines(userId, projectId, page, pageSize, PipelineSortType.CREATE_TIME,
                ChannelCode.BS, viewId ?: PIPELINE_VIEW_ALL_PIPELINES, true, keywords))
    }

    override fun list(userId: String, projectId: String, request: PipelineListRequest?): Result<List<Pipeline>> {
        return Result(txPipelineService.listPipelineInfo(userId, projectId, request))
    }
}