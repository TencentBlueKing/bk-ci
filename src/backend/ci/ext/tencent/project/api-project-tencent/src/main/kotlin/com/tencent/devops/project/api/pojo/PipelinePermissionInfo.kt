package com.tencent.devops.project.api.pojo

data class PipelinePermissionInfo (
    val userId: String,
    val projectId: String,
    val permissionList: List<String>
)