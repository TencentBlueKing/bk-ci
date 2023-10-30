package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("主机结构")
data class Host(
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bkCloudId")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bkHostId")
    val bkHostId: Long?,
    @ApiModelProperty(value = "IP地址")
    @JsonProperty("ip")
    val ip: String?
) {
    constructor(bkCloudId: Long?, ip: String?) : this(null, bkCloudId, ip)
    constructor(bkHostId: Long?) : this(bkHostId, null, null)
}