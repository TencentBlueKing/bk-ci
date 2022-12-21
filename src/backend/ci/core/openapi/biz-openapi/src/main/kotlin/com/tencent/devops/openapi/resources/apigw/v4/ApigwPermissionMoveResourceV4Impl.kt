package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwPermissionMoveResourceV4
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.project.api.service.ServiceMoveProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPermissionMoveResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwPermissionMoveResourceV4 {
    override fun relationProject(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        relationId: String
    ): com.tencent.devops.project.pojo.Result<Boolean> {
        logger.info("OPENAPI_PERMISSION_MOVE_V4|$appCode|relation project|$projectId|$relationId")
        return client.get(ServiceMoveProjectResource::class).relationIamProject(projectId, relationId)
    }

    override fun getProjectPipelineIds(
        appCode: String?,
        apigwType: String?,
        projectId: String
    ): Result<List<PipelineIdInfo>> {
        logger.info("OPENAPI_PERMISSION_MOVE_V4|$appCode|get project pipeline ids|$projectId")
        return client.get(ServicePipelineResource::class).getProjectPipelineIds(projectId)
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwPermissionMoveResourceV4Impl::class.java)
    }
}
