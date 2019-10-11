package com.tencent.devops.dispatch.pojo

data class DockerTemplate(
    val versionId: Int,
    val showVersionId: Int,
    val showVersionName: String,
    val deploymentId: Int,
    val deploymentName: String,
    val ccAppId: Long,
    val bcsProjectId: String,
    val clusterId: String,
    val createTime: Long
)