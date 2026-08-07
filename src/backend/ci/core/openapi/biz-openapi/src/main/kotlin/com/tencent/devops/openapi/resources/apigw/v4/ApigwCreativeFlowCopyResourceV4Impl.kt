package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwCreativeFlowCopyResourceV4
import com.tencent.devops.process.api.service.ServiceCreativeFlowCopyResource
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyResult
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyTraceVo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCreativeFlowCopyResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwCreativeFlowCopyResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwCreativeFlowCopyResourceV4Impl::class.java)
    }

    override fun copyAcrossProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: CreativeFlowCopyRequest
    ): Result<CreativeFlowCopyResult> {
        logger.info("OPENAPI_CREATIVE_FLOW_V4|$userId|copyAcrossProject|$projectId|shareId=${request.shareId}|flowId=${request.flowId}")
        return client.get(ServiceCreativeFlowCopyResource::class).copyAcrossProject(
            userId = userId,
            projectId = projectId,
            request = request
        )
    }

    override fun listCopyTraces(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        shareId: String?,
        flowId: String?,
        targetPipelineId: String?
    ): Result<List<CreativeFlowCopyTraceVo>> {
        logger.info("OPENAPI_CREATIVE_FLOW_V4|$userId|listCopyTraces|$projectId|shareId=$shareId")
        return client.get(ServiceCreativeFlowCopyResource::class).listCopyTraces(
            userId = userId,
            projectId = projectId,
            shareId = shareId,
            flowId = flowId,
            targetPipelineId = targetPipelineId
        )
    }
}
