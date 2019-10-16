package com.tencent.devops.support.model.mta.h5.result

import com.tencent.devops.support.model.mta.h5.base.IdxResult
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("MTA的H5接口请求返回结果")
data class CoreDataResult(
    @ApiModelProperty("状态码")
    val code: Int,
    @ApiModelProperty("消息")
    val info: String,
    @ApiModelProperty("内容")
    val data: Map<String, IdxResult>?
)