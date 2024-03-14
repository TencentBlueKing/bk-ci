package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "主机结构")
data class Host(
    @get:Schema(title = "云区域ID")
    val bkCloudId: Long?,
    @get:Schema(title = "主机ID")
    val bkHostId: Long?,
    @get:Schema(title = "IP地址")
    val ip: String?
) {
    constructor(bkHostId: Long?) : this(0, bkHostId, null)
    constructor(bkCloudId: Long?, ip: String?) : this(bkCloudId, null, ip)
}