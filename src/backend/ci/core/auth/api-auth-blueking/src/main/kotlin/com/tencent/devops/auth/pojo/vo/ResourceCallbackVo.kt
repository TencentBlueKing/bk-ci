package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.ResourceEntity

data class ResourceCallbackVo (
    val count: Long,
    val result: List<ResourceEntity>
)