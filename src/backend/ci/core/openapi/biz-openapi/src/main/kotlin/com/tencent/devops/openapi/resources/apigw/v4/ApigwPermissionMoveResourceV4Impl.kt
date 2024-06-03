package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwPermissionMoveResourceV4
import com.tencent.devops.openapi.service.OpenapiPermissionService
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.project.api.service.ServiceMoveProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPermissionMoveResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val openapiPermissionService: OpenapiPermissionService
) : ApigwPermissionMoveResourceV4 {
    override fun relationProject(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        relationId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_PERMISSION_MOVE_V4|$appCode|$userId|relation project|$projectId|$relationId")

        openapiPermissionService.validProjectManagerPermission(appCode, apigwType, userId, projectId)
        return Result(client.get(ServiceMoveProjectResource::class).relationIamProject(projectId, relationId).data!!)
    }

    override fun getProjectPipelineIds(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String
    ): Result<List<PipelineIdInfo>> {
        logger.info("OPENAPI_PERMISSION_MOVE_V4|$appCode|$userId|get project pipeline ids|$projectId")
        return client.get(ServicePipelineResource::class).getProjectPipelineIds(projectId)
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwPermissionMoveResourceV4Impl::class.java)
    }
}
