package com.tencent.devops.dispatch.codecc.pojo.devcloud

data class DevCloudJob(
    val regionId: String,
    val clusterType: String,
    val alias: String,
    val activeDeadlineSeconds: Int,
    val image: String,
    val registry: Registry,
    val cpu: Int,
    val memory: String,
    val params: Params?
)
