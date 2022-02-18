package com.tencent.devops.common.api.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.Api
import io.swagger.annotations.ApiModelProperty

/**
 *
 * Powered By Tencent
 */
@Api("数据返回包装模型")
data class Result<out T>(
    @ApiModelProperty("状态码", required = true)
    val status: Int,
    @ApiModelProperty("错误信息", required = false)
    val message: String? = null,
    @ApiModelProperty("数据", required = false)
    val data: T? = null
) {
    constructor(data: T) : this(0, null, data)
    constructor(status: Int, message: String) : this(status, message, null)

    @JsonIgnore
    fun isOk(): Boolean {
        return status == 0
    }

    @JsonIgnore
    fun isNotOk(): Boolean {
        return status != 0
    }
}
