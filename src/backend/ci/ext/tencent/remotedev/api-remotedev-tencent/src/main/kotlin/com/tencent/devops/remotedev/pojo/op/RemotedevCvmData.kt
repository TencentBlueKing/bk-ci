package com.tencent.devops.remotedev.pojo.op

import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiParam

data class RemotedevCvmData(
    @ApiModelProperty("ID")
    val id: Int?,
    @ApiParam(value = "项目ID", required = true)
    val projectId: String,
    @ApiParam(value = "区域", required = true)
    val zone: String?,
    @ApiParam(value = "可用区域", required = true)
    val availableRegion: String?,
    @ApiParam(value = "cpu", required = true)
    val cpu: Int?,
    @ApiParam(value = "内存", required = true)
    val memory: Int?,
    @ApiParam(value = "子网", required = true)
    val subnet: String?,
    @ApiParam(value = "云桌面IP", required = true)
    val ip: String
)
