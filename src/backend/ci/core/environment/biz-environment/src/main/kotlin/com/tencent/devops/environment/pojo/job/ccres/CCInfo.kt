package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCInfo(
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Int?,
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Int?,
    @ApiModelProperty(value = "内网IP")
    @JsonProperty("bk_host_innerip")
    val bkHostInnerip: String?,
    @ApiModelProperty(value = "主负责人")
    @JsonProperty("operator")
    val operator: String?,
    @ApiModelProperty(value = "备份负责人", notes = "不同备份负责人之间用英文逗号隔开")
    @JsonProperty("bk_bak_operator")
    val bkBakOperator: String?
)