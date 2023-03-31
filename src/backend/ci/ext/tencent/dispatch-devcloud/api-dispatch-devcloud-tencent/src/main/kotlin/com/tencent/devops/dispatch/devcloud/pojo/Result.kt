package com.tencent.devops.dispatch.devcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("数据返回包装模型")
data class Result<out T>(
    @ApiModelProperty("状态码", required = true)
    val code: Int,
    @ApiModelProperty("错误信息", required = false)
    val message: String? = null,
    @ApiModelProperty("数据", required = false)
    val data: T? = null,
    @ApiModelProperty("请求ID", required = false)
    val requestId: String? = null,
    @ApiModelProperty("请求结果", required = false)
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
