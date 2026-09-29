package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "请求上云版job v4接口的正常响应")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudV4Resp<T>(
    @get:Schema(title = "响应数据")
    @JsonProperty("data")
    val data: T? = null
)

@Schema(title = "请求上云版job v4接口的异常响应")
@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudV4ErrorResp(
    @get:Schema(title = "错误信息")
    @JsonProperty("error")
    val error: JobCloudV4Error? = null
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
