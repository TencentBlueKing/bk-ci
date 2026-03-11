package com.tencent.devops.remotedev.pojo.tcloud

data class ProjectCfsData(
    val projectId: String,
    val cfsId: String,
    val region: String
)

data class UpdateCfsData(
    val projectId: String,
    val cfsId: String,
    val ips: Set<String>,
    val remove: Boolean
)