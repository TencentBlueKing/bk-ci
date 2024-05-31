package com.tencent.devops.environment.pojo.job.cmdbres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CmdbResp(
    @get:Schema(title = "状态码", required = true)
    var code: Int,
    @get:Schema(title = "执行成功失败", required = true)
    var result: Boolean,
    @get:Schema(title = "请求链ID", required = true)
    @JsonProperty("request_id")
    var requestId: String,
    @get:Schema(title = "结果消息", required = true)
    var message: String?,
    @get:Schema(title = "返回数据", required = true)
    var data: CmdbData
)