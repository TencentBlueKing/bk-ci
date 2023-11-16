package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCDeleteHostReq(
    @ApiModelProperty(value = "要新增的公司cmdb主机ID数组", notes = "一次最多新增200台主机。")
    @JsonProperty("bk_host_ids")
    val bkHostIds: Set<Long>
)