package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.annotation.SkipLogField
import io.swagger.annotations.ApiModelProperty

data class CCListHostWithoutBizReq<T>(
    @ApiModelProperty(value = "应用ID (app id)", required = true)
    @JsonProperty("bk_app_code")
    val bkAppCode: String? = "",
    @ApiModelProperty(value = "安全秘钥 (app secret)", required = true)
    @JsonProperty("bk_app_secret")
    @SkipLogField val bkAppSecret: String? = "",
    @ApiModelProperty(value = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    val bkUsername: String? = "admin",
    @ApiModelProperty(value = "提供者账户，默认'tencent'", required = true)
    @JsonProperty("bk_supplier_account")
    val bkSupplierAccount: String? = "",
    @ApiModelProperty(value = "查询条件", required = true)
    @JsonProperty("page")
    val page: CCPage,
    @ApiModelProperty(value = "主机属性列表", notes = "控制返回结果的主机里有哪些字段，能够加速接口请求和减少网络流量传输", required = true)
    @JsonProperty("fields")
    val fields: List<String>,
    @ApiModelProperty(value = "主机属性组合查询条件", required = true)
    @JsonProperty("host_property_filter")
    val hostPropertyFilter: CCHostPropertyFilter<T>?
)