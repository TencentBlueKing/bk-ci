package com.tencent.devops.environment.pojo.job

@Suppress("ALL")
data class HostJobCloudReq(
    var bk_host_id: Long?,
    var bk_cloud_id: Long?,
    var ip: String?
)