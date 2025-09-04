package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCDeleteHostReq(
    @get:Schema(title = "要新增的公司cmdb主机ID数组", description = "一次最多新增200台主机。")
    @JsonProperty("bk_host_ids")
    val bkHostIds: Set<Long>
)