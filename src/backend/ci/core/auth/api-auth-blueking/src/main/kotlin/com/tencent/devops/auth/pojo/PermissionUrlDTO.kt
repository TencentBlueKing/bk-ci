package com.tencent.devops.auth.pojo

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.pojo.Instance

data class PermissionUrlDTO(
    val actionId: AuthPermission,
    val resourceId: AuthResourceType,
    val instanceId: List<Instance>?
)