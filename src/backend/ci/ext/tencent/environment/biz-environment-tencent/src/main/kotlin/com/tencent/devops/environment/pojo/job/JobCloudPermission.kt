package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
abstract class JobCloudPermission(
    @ApiModelProperty(value = "应用ID", required = true)
    @JsonProperty("bk_app_code")
    open var bkAppCode: String,
    @ApiModelProperty(value = "安全秘钥", required = true)
    @JsonProperty("bk_app_secret")
    open var bkAppSecret: String,
    @ApiModelProperty(value = "当前用户用户名", required = true)
    @JsonProperty("bk_username")
    open var bkUsername: String
)