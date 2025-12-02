package com.tencent.devops.auth.service

/**
 * 资源组获取类
 */
interface AuthResourceGroupService {
    fun supportResourceType(resourceType: String): Boolean

    fun getResourcesUnderGroup(projectCode: String, resourceGroupIds: List<String>): List<String>

    fun getResourceGroupsByResource(projectCode: String, resourceCode: String): List<String>
}
