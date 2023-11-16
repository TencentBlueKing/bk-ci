package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCResp<out T>(
    @ApiModelProperty(value = "状态码", required = true)
    @JsonProperty("code")
    val code: Int,
    @ApiModelProperty(value = "状态码名称")
    @JsonProperty("code_name")
    val codeName: String?,
    @ApiModelProperty(value = "权限信息")
    @JsonProperty("permission")
    val permission: String?,
    @ApiModelProperty(value = "执行成功失败")
    @JsonProperty("result")
    val result: Boolean?,
    @ApiModelProperty(value = "请求链ID")
    @JsonProperty("request_id")
    val requestId: String?,
    @ApiModelProperty(value = "结果消息")
    @JsonProperty("message")
    val message: String?,
    @ApiModelProperty(value = "返回数据")
    @JsonProperty("data")
    val data: T? = null
)