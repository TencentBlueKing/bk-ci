package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Job数据返回包装模型")
data class JobCloudResult<out T>(
    @get:Schema(title = "状态码", required = true)
    val code: Int,
    @get:Schema(title = "结果bool值")
    val result: Boolean? = null,
    @get:Schema(title = "请求ID", required = true)
    @JsonProperty("job_request_id")
    val jobRequestId: String?,
    @get:Schema(title = "数据")
    @BkFieldI18n
    val data: T? = null
)