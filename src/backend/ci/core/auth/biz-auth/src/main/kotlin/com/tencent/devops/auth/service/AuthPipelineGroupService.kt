package com.tencent.devops.auth.service

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineViewResource
import org.springframework.stereotype.Service

@Service
class AuthPipelineGroupService(private val client: Client) : AuthResourceGroupService {
    override fun supportResourceType(resourceType: String): Boolean {
        return resourceType == ResourceTypeId.PIPELINE_GROUP
    }

    override fun getResourcesUnderGroup(projectCode: String, resourceGroupIds: List<String>): List<String> {
        return client.get(ServicePipelineViewResource::class).listPipelineIdByViewIds(
            projectId = projectCode,
            viewIdsEncode = resourceGroupIds
        ).data ?: emptyList()
    }

    override fun getResourceGroupsByResource(projectCode: String, resourceCode: String): List<String> {
        return client.get(ServicePipelineViewResource::class).listViewIdsByPipelineId(
            projectId = projectCode,
            pipelineId = resourceCode
        ).data?.map { HashUtil.encodeLongId(it) } ?: emptyList()
    }
}
