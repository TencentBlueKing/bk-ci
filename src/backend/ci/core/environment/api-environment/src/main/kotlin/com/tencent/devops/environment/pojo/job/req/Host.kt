package com.tencent.devops.environment.pojo.job.req

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("主机结构")
data class Host(
    @ApiModelProperty(value = "云区域ID")
    val bkCloudId: Long? = 0,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?
) {
    constructor(bkHostId: Long?) : this(0, bkHostId, null)
    constructor(bkCloudId: Long?, ip: String?) : this(bkCloudId, null, ip)
}