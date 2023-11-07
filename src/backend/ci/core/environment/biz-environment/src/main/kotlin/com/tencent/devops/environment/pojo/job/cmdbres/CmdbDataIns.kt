package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class CmdbDataIns(
    @ApiModelProperty(value = "主负责人", required = true)
    val SvrOperator: String,
    @ApiModelProperty(value = "备份负责人", required = true)
    val SvrBakOperator: String,
    @ApiModelProperty(value = "服务器名称", required = true)
    val SvrName: String,
    @ApiModelProperty(value = "服务器局域网IP", required = true)
    val serverLanIP: List<String>,
    @ApiModelProperty(value = "服务器IP", required = true)
    val SvrIp: String,
    @ApiModelProperty(value = "服务器操作系统", required = true)
    val SfwName: String,
    @ApiModelProperty(value = "服务器ID", required = true)
    val serverId: Int
)