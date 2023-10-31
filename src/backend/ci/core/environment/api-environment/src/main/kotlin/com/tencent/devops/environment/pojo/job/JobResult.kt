package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Job数据返回包装模型")
data class JobResult<out T>(
    @ApiModelProperty("状态码", required = true)
    val code: Int,
    @ApiModelProperty("结果bool值")
    val result: Boolean? = null,
    @ApiModelProperty("请求ID", required = true)
    @get:JsonProperty("job_request_id")
    val jobRequestId: String?,
    @ApiModelProperty("数据")
    @BkFieldI18n
    val data: T? = null
) {
    constructor(data: T) : this(0, true, null, data)
}