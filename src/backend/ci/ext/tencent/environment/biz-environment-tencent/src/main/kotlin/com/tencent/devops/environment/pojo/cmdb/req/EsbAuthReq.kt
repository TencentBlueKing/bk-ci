package com.tencent.devops.environment.pojo.cmdb.req

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 调用ESB基础认证信息
 */
data class EsbAuthReq(
    /**
     * 接口调用AppCode
     */
    @JsonProperty("bk_app_code")
    val appCode: String,

    /**
     * 接口调用密钥
     */
    @JsonProperty("bk_app_secret")
    val appSecret: String
)
