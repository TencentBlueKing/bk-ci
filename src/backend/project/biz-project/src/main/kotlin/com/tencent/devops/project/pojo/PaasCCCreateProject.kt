package com.tencent.devops.project.pojo

data class PaasCCCreateProject(
    val userId: String,
    val accessToken: String,
    val projectId: String,
    val retryCount: Int,
    val projectCreateInfo: ProjectCreateInfo
)