package com.tencent.devops.environment.pojo.job.cmdbreq

import com.fasterxml.jackson.annotation.JsonProperty

import io.swagger.v3.oas.annotations.media.Schema

data class CmdbGetQueryInfoReq(
    @get:Schema(title = "应用ID (app id)", required = true)
    @JsonProperty("app_code")
    val bkAppCode: String? = "",
    @get:Schema(title = "安全秘钥 (app secret)", required = true)
    @JsonProperty("app_secret")
    val bkAppSecret: String? = "",
    @get:Schema(title = "操作人")
    @JsonProperty("operator")
    val operator: String?,
    @get:Schema(title = "请求列", required = true)
    @JsonProperty("req_column")
    val reqColumn: List<String>,
    @get:Schema(title = "请求条件")
    @JsonProperty("key_values")
    val keyValues: CmdbKeyValues?,
    @get:Schema(title = "分页信息")
    @JsonProperty("paging_info")
    val pagingInfo: CmdbPagingInfo?
)