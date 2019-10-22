package com.tencent.devops.project.pojo

data class PaasCCUpdateProject(
    val userId: String,
    val accessToken: String,
    val projectId: String,
    val retryCount: Int,
    val projectUpdateInfo: ProjectUpdateInfo
)