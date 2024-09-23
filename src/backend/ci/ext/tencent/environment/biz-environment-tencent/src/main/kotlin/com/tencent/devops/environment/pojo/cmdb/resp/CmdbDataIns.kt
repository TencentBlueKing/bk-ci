package com.tencent.devops.environment.pojo.cmdb.resp

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
data class CmdbDataIns(
    @get:Schema(title = "主负责人")
    @JsonProperty("SvrOperator")
    val svrOperator: String?,
    @get:Schema(title = "备份负责人")
    @JsonProperty("SvrBakOperator")
    val svrBakOperator: String?,
    @get:Schema(title = "服务器名称")
    @JsonProperty("SvrName")
    val svrName: String?,
    @get:Schema(title = "服务器局域网IP")
    @JsonProperty("serverLanIP")
    val svrLanIP: List<String>?,
    @get:Schema(title = "服务器IP")
    @JsonProperty("SvrIp")
    val svrIp: String?,
    @get:Schema(title = "服务器操作系统")
    @JsonProperty("SfwName")
    val svrOsName: String?,
    @get:Schema(title = "服务器ID")
    val serverId: Long?,
    @get:Schema(title = "运维部门ID")
    @JsonProperty("DeptId")
    val deptId: Int?
)
