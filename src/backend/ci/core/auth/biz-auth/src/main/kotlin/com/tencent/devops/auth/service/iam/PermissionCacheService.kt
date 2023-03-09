package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo

interface PermissionCacheService {
    fun listResourceTypes(): List<ResourceTypeInfoVo>

    fun listResourceType2Action(resourceType: String): List<ActionInfoVo>

    fun getActionInfo(action: String): ActionInfoVo

    fun getResourceTypeInfo(resourceType: String): ResourceTypeInfoVo
}
