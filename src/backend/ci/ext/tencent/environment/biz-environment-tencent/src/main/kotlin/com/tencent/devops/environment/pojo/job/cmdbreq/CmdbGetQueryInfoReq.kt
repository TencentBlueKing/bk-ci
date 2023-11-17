package com.tencent.devops.environment.pojo.job.cmdbreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CmdbGetQueryInfoReq(
    @ApiModelProperty(value = "应用ID (app id)", required = true)
    @JsonProperty("app_code")
    val bkAppCode: String? = "",
    @ApiModelProperty(value = "安全秘钥 (app secret)", required = true)
    @JsonProperty("app_secret")
    val bkAppSecret: String? = "",
    @ApiModelProperty(value = "操作人")
    @JsonProperty("operator")
    val operator: String?,
    @ApiModelProperty(value = "请求列", required = true)
    @JsonProperty("req_column")
    val reqColumn: List<String>,
    @ApiModelProperty(value = "请求条件")
    @JsonProperty("key_values")
    val keyValues: CmdbKeyValues?,
    @ApiModelProperty(value = "分页信息")
    @JsonProperty("paging_info")
    val pagingInfo: CmdbPagingInfo?
)