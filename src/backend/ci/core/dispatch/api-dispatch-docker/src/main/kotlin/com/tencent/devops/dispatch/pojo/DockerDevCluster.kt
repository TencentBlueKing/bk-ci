package com.tencent.devops.dispatch.pojo

data class DockerDevCluster(
    var clusterId: String?,
    val clusterName: String,
    val enable: Boolean
)