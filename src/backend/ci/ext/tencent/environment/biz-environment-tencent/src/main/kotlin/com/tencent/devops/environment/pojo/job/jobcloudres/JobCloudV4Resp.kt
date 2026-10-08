package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "请求上云版job v4接口的响应")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudV4Resp<T>(
    @get:Schema(title = "响应数据，HTTP状态码为2xx时返回，异常时为null")
    @JsonProperty("data")
    val data: T? = null,
    @get:Schema(title = "错误信息，HTTP状态码非2xx时返回，正常时为null")
    @JsonProperty("error")
    val error: JobCloudV4Error? = null,
    @get:Schema(title = "Job平台的请求ID")
    @JsonProperty("job_request_id")
    val jobRequestId: String? = null
)

@Schema(title = "上云版job v4接口的错误信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudV4Error(
    @get:Schema(title = "错误码", description = "如 INVALID_ARGUMENT、NO_PERMISSION")
    @JsonProperty("code")
    val code: String? = null,
    @get:Schema(title = "错误信息")
    @JsonProperty("message")
    val message: String? = null
)
