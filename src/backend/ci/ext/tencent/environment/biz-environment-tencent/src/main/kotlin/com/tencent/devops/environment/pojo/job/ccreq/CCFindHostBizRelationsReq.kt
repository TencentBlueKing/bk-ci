package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.annotation.SkipLogField

import io.swagger.v3.oas.annotations.media.Schema

data class CCFindHostBizRelationsReq(
    @get:Schema(title = "应用ID (app id)", required = true)
    @JsonProperty("bk_app_code")
    val bkAppCode: String? = "",
    @get:Schema(title = "安全秘钥 (app secret)", required = true)
    @JsonProperty("bk_app_secret")
    @SkipLogField val bkAppSecret: String? = "",
    @get:Schema(title = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    val bkUsername: String? = "admin",
    @get:Schema(title = "主机属性列表", required = true)
    @JsonProperty("bk_host_id")
    val bkHostId: List<Long>
)