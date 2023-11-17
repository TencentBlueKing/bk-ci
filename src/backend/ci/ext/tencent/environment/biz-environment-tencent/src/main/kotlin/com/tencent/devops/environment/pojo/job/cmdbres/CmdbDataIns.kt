package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class CmdbDataIns(
    @ApiModelProperty(value = "主负责人")
    val SvrOperator: String?,
    @ApiModelProperty(value = "备份负责人")
    val SvrBakOperator: String?,
    @ApiModelProperty(value = "服务器名称")
    val SvrName: String?,
    @ApiModelProperty(value = "服务器局域网IP")
    val serverLanIP: List<String>?,
    @ApiModelProperty(value = "服务器IP")
    val SvrIp: String?,
    @ApiModelProperty(value = "服务器操作系统")
    val SfwName: String?,
    @ApiModelProperty(value = "服务器ID")
    val serverId: Int?
)