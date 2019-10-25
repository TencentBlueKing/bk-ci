package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.external.ServiceMeasureResource
import com.tencent.devops.openapi.api.v2.ApigwPipelineResourceV2
import com.tencent.devops.openapi.pojo.BuildStatisticsResponse
import com.tencent.devops.openapi.service.v2.ApigwPipelineServiceV2
import com.tencent.devops.process.api.v2.ServicePipelineResourceV2
import com.tencent.devops.process.pojo.Pipeline
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineResourceV2Impl @Autowired constructor(
    private val client: Client,
    private val apigwPipelineResourceService: ApigwPipelineServiceV2
) : ApigwPipelineResourceV2 {

    override fun getListByOrganization(userId: String, organizationType: String, organizationName: String, deptName: String?, centerName: String?, page: Int?, pageSize: Int?): Result<Page<Pipeline>> {
        return apigwPipelineResourceService.getListByOrganization(
            userId = userId,
            organizationType = organizationType,
            organizationName = organizationName,
            deptName = deptName,
            centerName = centerName,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getListByBuildResource(userId: String, buildResourceType: String, buildResourceValue: String?, page: Int?, pageSize: Int?): Result<Page<Pipeline>> {
        // 1.直接调Process微服务根据构建资源查流水线接口获取结果返回
        return client.getWithoutRetry(ServicePipelineResourceV2::class).listPipelinesByBuildResource(
            userId = userId,
            buildResourceType = buildResourceType,
            buildResourceValue = buildResourceValue,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS
        )
    }

    override fun buildStatistics(userId: String, organizationType: String, organizationId: Int, deptName: String?, centerName: String?, beginTime: String?, endTime: String?, type: String?): Result<BuildStatisticsResponse> {
        // 1.直接调Measure微服务查询流水线构建统计数据
        return client.getWithoutRetry(ServiceMeasureResource::class).buildStatistics(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName,
            centerName = centerName,
            beginTime = beginTime,
            endTime = endTime,
            type = type
        )
    }
}