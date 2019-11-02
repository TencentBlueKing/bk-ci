package com.tencent.devops.process.api.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.service.PipelineContainerDispatchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineResourceImplV2 @Autowired constructor(
    private val pipelineContainerDispatchService: PipelineContainerDispatchService
) : ServicePipelineResourceV2 {
    override fun extractDispatchTypeByProjectId(userId: String, projectId: String): Result<String> {
        return Result(pipelineContainerDispatchService.extractDispatchTypeByProjectId(
            userId = userId,
            projectId = projectId,
            interfaceName = "/service/v2/pipelines/dispatchTypeExtract"
        ))
    }

    override fun listPipelinesByBuildResource(userId: String, buildResourceType: String, buildResourceValue: String?, page: Int?, pageSize: Int?, channelCode: ChannelCode?): Result<Page<Pipeline>> {
        return Result(pipelineContainerDispatchService.listPipelinesByDispatch(
            dispatchBuildType = buildResourceType,
            dispatchValue = buildResourceValue,
            channelCode = ChannelCode.BS,
            page = page,
            pageSize = pageSize,
            sortType = PipelineSortType.UPDATE_TIME
        ))
    }
}