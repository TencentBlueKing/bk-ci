package com.tencent.devops.project.pojo

data class PaasCCUpdateProjectLogo(
    val userId: String,
    val accessToken: String,
    val projectId: String,
    val retryCount: Int,
    val projectUpdateLogoInfo: ProjectUpdateLogoInfo
)