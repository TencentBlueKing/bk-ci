package com.tencent.devops.environment.pojo.apigw

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 调用ApiGw基础认证信息
 */
data class ApiGwBasicReq(
    /**
     * 接口调用AppCode
     */
    @JsonProperty("bk_app_code")
    val appCode: String,

    /**
     * 接口调用密钥
     */
    @JsonProperty("bk_app_secret")
    val appSecret: String,

    /**
     * 用户名
     */
    @JsonProperty("bk_username")
    val username: String?
)
