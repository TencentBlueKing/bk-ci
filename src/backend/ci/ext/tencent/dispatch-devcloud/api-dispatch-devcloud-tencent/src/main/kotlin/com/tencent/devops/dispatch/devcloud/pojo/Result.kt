package com.tencent.devops.dispatch.devcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "数据返回包装模型")
data class Result<out T>(
    @get:Schema(title = "状态码", required = true)
    val code: Int,
    @get:Schema(title = "错误信息", required = false)
    val message: String? = null,
    @get:Schema(title = "数据", required = false)
    val data: T? = null,
    @get:Schema(title = "请求ID", required = false)
    val requestId: String? = null,
    @get:Schema(title = "请求结果", required = false)
    val result: Boolean? = null
) {
    constructor(data: T) : this(0, null, data)
    constructor(message: String, data: T) : this(0, message, data)
    constructor(status: Int, message: String?, request_id: String?, result: Boolean) :
        this(status, message, null, request_id, result)
    @JsonIgnore
    fun isOk(): Boolean {
        return code == 0
    }

    @JsonIgnore
    fun isNotOk(): Boolean {
        return code != 0
    }
}
