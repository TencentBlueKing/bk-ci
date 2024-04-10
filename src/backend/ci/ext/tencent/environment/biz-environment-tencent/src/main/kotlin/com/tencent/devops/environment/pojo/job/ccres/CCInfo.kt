package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCInfo(
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Int?,
    @get:Schema(title = "内网IP")
    @JsonProperty("bk_host_innerip")
    val bkHostInnerip: String?,
    @get:Schema(title = "主负责人")
    @JsonProperty("operator")
    val operator: String?,
    @get:Schema(title = "备份负责人", description = "不同备份负责人之间用英文逗号隔开")
    @JsonProperty("bk_bak_operator")
    val bkBakOperator: String?,
    @get:Schema(title = "服务器ID")
    @JsonProperty("svr_id")
    val svrId: Long?,
    @get:Schema(title = "操作系统类型")
    @JsonProperty("bk_os_type")
    var osType: String?
) {
    constructor(bkHostId: Long?, bkCloudId: Int?, bkHostInnerip: String?, svrId: Long?, osType: String?) :
        this(bkHostId, bkCloudId, bkHostInnerip, "", "", svrId, osType)
}