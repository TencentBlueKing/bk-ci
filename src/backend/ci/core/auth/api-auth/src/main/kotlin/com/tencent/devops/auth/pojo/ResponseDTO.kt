package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("请求返回实体")
data class ResponseDTO(
    @ApiModelProperty("返回码")
    val code: Long,
    @ApiModelProperty("返回信息")
    val message: String,
    @ApiModelProperty("请求返回结果")
    val result: Boolean,
    @ApiModelProperty("请求返回数据")
    val data: Any?
)
