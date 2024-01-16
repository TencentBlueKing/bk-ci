package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class MoaCredentialKeyVerifyResponse(
    @ApiModelProperty("请求返回标识")
    @JsonProperty("ReturnFlag")
    val returnFlag: Int,
    @ApiModelProperty("请求返回信息")
    @JsonProperty("msg")
    val msg: String?,
    @ApiModelProperty("用户名称")
    @JsonProperty("EnglishName")
    val userId: String?
)
