package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCAddHostReq(
    @get:Schema(title = "要新增的公司cmdb主机ID数组", description = "一次最多新增200台主机。")
    @JsonProperty("svr_ids")
    val svrIds: List<Long>
)