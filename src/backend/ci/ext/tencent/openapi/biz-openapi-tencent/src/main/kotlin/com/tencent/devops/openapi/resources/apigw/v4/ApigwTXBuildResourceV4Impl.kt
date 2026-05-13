package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXBuildResourceV4
import com.tencent.devops.process.api.service.ServiceTXBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.pipeline.IMateBuildStartRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXBuildResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwTXBuildResourceV4 {

    override fun iMateBuildStart(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        request: IMateBuildStartRequest
    ): Result<BuildId> {
        logger.info("OPENAPI_TX_BUILD_V4|$userId|IMate build start|$projectId|$pipelineId")
        return client.get(ServiceTXBuildResource::class).iMateBuildStart(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            request = request
        )
    }

    override fun visibilityManualStartupInfo(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        logger.info("OPENAPI_TX_BUILD_V4|$userId|visibility startup info|$projectId|$pipelineId")
        return client.get(ServiceTXBuildResource::class).visibilityManualStartupInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTXBuildResourceV4Impl::class.java)
    }
}
