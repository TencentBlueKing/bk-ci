package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.jooq.types.ULong

@ApiModel("主机结构")
data class Host(
    @ApiModelProperty(value = "云区域ID")
    val bkCloudId: ULong?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: ULong?
)