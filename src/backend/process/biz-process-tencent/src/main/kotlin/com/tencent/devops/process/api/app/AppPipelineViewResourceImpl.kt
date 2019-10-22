package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.service.view.PipelineViewService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppPipelineViewResourceImpl @Autowired constructor(
    private val pipelineService: PipelineService,
    private val pipelineViewService: PipelineViewService
) : AppPipelineViewResource {
    override fun listViewPipelines(userId: String, projectId: String, page: Int?, pageSize: Int?, sortType: PipelineSortType?, filterByPipelineName: String?, filterByCreator: String?, filterByLabels: String?, viewId: String): Result<PipelineViewPipelinePage<Pipeline>> {
        return Result(pipelineService.listViewPipelines(userId, projectId, page, pageSize, sortType ?: PipelineSortType.CREATE_TIME,
                ChannelCode.BS, viewId, true, filterByPipelineName, filterByCreator, filterByLabels))
    }

    override fun getViewSettings(userId: String, projectId: String): Result<PipelineViewSettings> {
        return Result(pipelineViewService.getViewSettings(userId, projectId))
    }
}