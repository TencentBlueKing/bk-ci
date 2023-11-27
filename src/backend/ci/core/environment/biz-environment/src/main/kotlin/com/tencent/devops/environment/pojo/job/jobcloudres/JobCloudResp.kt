package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("请求上云版job接口的返回")
data class JobCloudResp<T>(
    @ApiModelProperty(value = "状态码", required = true)
    @JsonProperty("code")
    var code: Int,
    @ApiModelProperty(value = "执行成功失败", required = true)
    @JsonProperty("result")
    var result: Boolean,
    @ApiModelProperty(value = "请求ID")
    @JsonProperty("job_request_id")
    var jobRequestId: String?,
    @ApiModelProperty(value = "结果消息", required = true)
    @JsonProperty("message")
    var message: String?,
    @ApiModelProperty(value = "返回数据", required = true)
    @JsonProperty("data")
    var data: T? = null
)