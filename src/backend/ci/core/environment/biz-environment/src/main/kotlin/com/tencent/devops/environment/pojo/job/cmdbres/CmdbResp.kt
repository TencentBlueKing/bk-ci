package com.tencent.devops.environment.pojo.job.cmdbres

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.environment.pojo.job.ccres.CCData
import io.swagger.annotations.ApiModelProperty

data class CmdbResp (
    @ApiModelProperty(value = "状态码", required = true)
    var code: Int,
    @ApiModelProperty(value = "执行成功失败", required = true)
    var result: Boolean,
    @ApiModelProperty(value = "请求链ID", required = true)
    @JsonProperty("request_id")
    var requestId: String,
    @ApiModelProperty(value = "结果消息", required = true)
    var message: String?,
    @ApiModelProperty(value = "返回数据", required = true)
    var data: CmdbData
)