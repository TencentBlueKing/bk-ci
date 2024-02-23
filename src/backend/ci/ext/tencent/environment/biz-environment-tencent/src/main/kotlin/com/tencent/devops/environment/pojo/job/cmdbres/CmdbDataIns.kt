package com.tencent.devops.environment.pojo.job.cmdbres

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
data class CmdbDataIns(
    @get:Schema(title = "主负责人")
    val SvrOperator: String?,
    @get:Schema(title = "备份负责人")
    val SvrBakOperator: String?,
    @get:Schema(title = "服务器名称")
    val SvrName: String?,
    @get:Schema(title = "服务器局域网IP")
    val serverLanIP: List<String>?,
    @get:Schema(title = "服务器IP")
    val SvrIp: String?,
    @get:Schema(title = "服务器操作系统")
    val SfwName: String?,
    @get:Schema(title = "服务器ID")
    val serverId: Int?
)