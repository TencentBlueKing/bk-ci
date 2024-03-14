package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "请求上云版job接口的返回")
data class JobCloudResp<T>(
    @get:Schema(title = "状态码", required = true)
    @JsonProperty("code")
    var code: Int,
    @get:Schema(title = "执行成功失败", required = true)
    @JsonProperty("result")
    var result: Boolean,
    @get:Schema(title = "请求ID")
    @JsonProperty("job_request_id")
    var jobRequestId: String?,
    @get:Schema(title = "结果消息", required = true)
    @JsonProperty("message")
    var message: String?,
    @get:Schema(title = "返回数据", required = true)
    @JsonProperty("data")
    var data: T? = null
)