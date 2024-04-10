package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCResp<out T>(
    @get:Schema(title = "状态码", required = true)
    @JsonProperty("code")
    val code: Int,
    @get:Schema(title = "状态码名称")
    @JsonProperty("code_name")
    val codeName: String?,
    @get:Schema(title = "权限信息")
    @JsonProperty("permission")
    val permission: String?,
    @get:Schema(title = "执行成功失败")
    @JsonProperty("result")
    val result: Boolean?,
    @get:Schema(title = "请求链ID")
    @JsonProperty("request_id")
    val requestId: String?,
    @get:Schema(title = "结果消息")
    @JsonProperty("message")
    val message: String?,
    @get:Schema(title = "返回数据")
    @JsonProperty("data")
    val data: T? = null
)