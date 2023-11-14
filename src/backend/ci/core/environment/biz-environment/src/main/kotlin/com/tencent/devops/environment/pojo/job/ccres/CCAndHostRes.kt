package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCAndHostRes(
    @ApiModelProperty(value = "状态码", required = true)
    @JsonProperty("code")
    var code: Int,
    @ApiModelProperty(value = "状态码名称")
    @JsonProperty("code_name")
    var codeName: String?,
    @ApiModelProperty(value = "权限信息")
    @JsonProperty("permission")
    var permission: String?,
    @ApiModelProperty(value = "执行成功失败", required = true)
    @JsonProperty("result")
    var result: Boolean,
    @ApiModelProperty(value = "请求链ID")
    @JsonProperty("request_id")
    var requestId: String? = null,
    @ApiModelProperty(value = "结果消息", required = true)
    @JsonProperty("message")
    var message: String?,
    @ApiModelProperty(value = "返回数据")
    @JsonProperty("data")
    var data: CCBkHost? = null
)