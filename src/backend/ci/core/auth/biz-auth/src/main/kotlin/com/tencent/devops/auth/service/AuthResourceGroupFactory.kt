package com.tencent.devops.auth.service

import com.tencent.devops.common.auth.api.ResourceTypeId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AuthResourceGroupFactory(private val authResourceGroupServices: List<AuthResourceGroupService>) {
    fun getResourcesUnderGroup(
        resourceGroupType: String,
        projectCode: String,
        resourceGroupIds: List<String>
    ): List<String> {
        return try {
            if (resourceGroupIds.isEmpty()) {
                return emptyList()
            }
            authResourceGroupServices
                .firstOrNull { it.supportResourceType(resourceGroupType) }
                ?.getResourcesUnderGroup(projectCode, resourceGroupIds)
                ?: emptyList()
        } catch (e: Exception) {
            // 记录错误或根据业务需求进行处理
            logger.info("get resource group error: $projectCode|$resourceGroupType|$resourceGroupIds}", e)
            emptyList()
        }
    }

    fun getResourceGroupsByResource(
        resourceGroupType: String,
        projectCode: String,
        resourceCode: String
    ): List<String> {
        return try {
            authResourceGroupServices
                .firstOrNull { it.supportResourceType(resourceGroupType) }
                ?.getResourceGroupsByResource(projectCode, resourceCode)
                ?: emptyList()
        } catch (e: Exception) {
            // 记录错误或根据业务需求进行处理
            logger.info("get resource groups by resource error:$projectCode|$resourceGroupType|$resourceCode", e)
            emptyList()
        }
    }

    fun getResourceGroupType(resourceType: String): String? {
        return resourceType2ResourceGroupType[resourceType]
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthResourceGroupFactory::class.java)
        private val resourceType2ResourceGroupType = mapOf(
            ResourceTypeId.PIPELINE to ResourceTypeId.PIPELINE_GROUP,
            ResourceTypeId.CGS to ResourceTypeId.CGS_GROUP
        )
    }
}
