package com.tencent.devops.auth.service

import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.api.ServiceWorkspaceGroupResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthWorkSpaceGroupService(private val client: Client) : AuthResourceGroupService {
    override fun supportResourceType(resourceType: String): Boolean {
        return resourceType == ResourceTypeId.CGS_GROUP
    }

    override fun getResourcesUnderGroup(projectCode: String, resourceGroupIds: List<String>): List<String> {
        logger.info("getResourcesUnderGroup: projectCode=$projectCode, resourceGroupIds=$resourceGroupIds")
        if (resourceGroupIds.isEmpty()) {
            return emptyList()
        }
        
        val groupIds = resourceGroupIds.mapNotNull { it.toLongOrNull() }
        if (groupIds.isEmpty()) {
            logger.warn("Invalid resourceGroupIds: $resourceGroupIds")
            return emptyList()
        }
        
        return try {
            val result = client.get(ServiceWorkspaceGroupResource::class)
                .getWorkspacesByGroups(projectCode, groupIds)
            result.data ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to get workspaces under groups: projectCode=$projectCode, groupIds=$groupIds", e)
            emptyList()
        }
    }

    override fun getResourceGroupsByResource(projectCode: String, resourceCode: String): List<String> {
        logger.info("getResourceGroupsByResource: projectCode=$projectCode, resourceCode=$resourceCode")
        
        return try {
            val result = client.get(ServiceWorkspaceGroupResource::class)
                .getGroupsByWorkspace(projectCode, resourceCode)
            result.data?.map { it.toString() } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to get groups by workspace: projectCode=$projectCode, resourceCode=$resourceCode", e)
            emptyList()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthWorkSpaceGroupService::class.java)
    }
}
