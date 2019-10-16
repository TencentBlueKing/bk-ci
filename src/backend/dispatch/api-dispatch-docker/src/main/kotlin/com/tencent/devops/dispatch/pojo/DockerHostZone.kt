package com.tencent.devops.dispatch.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * DockerHostZone
 */
@ApiModel("DockerHostZone")
data class DockerHostZone(
    @ApiModelProperty("hostIp", required = true)
    val hostIp: String,
    @ApiModelProperty("zone", required = true)
    val zone: String,
    @ApiModelProperty("enable", required = true)
    val enable: Boolean,
    @ApiModelProperty("remark", required = true)
    val remark: String?,
    @ApiModelProperty("createTime", required = true)
    val createTime: Long,
    @ApiModelProperty("updateTime", required = true)
    val updateTime: Long
)