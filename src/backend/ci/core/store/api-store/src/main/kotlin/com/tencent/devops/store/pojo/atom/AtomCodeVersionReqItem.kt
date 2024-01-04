package com.tencent.devops.store.pojo.atom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件代码版本请求对象")
data class AtomCodeVersionReqItem(
    @ApiModelProperty("插件代码", required = true)
    val atomCode: String,
    @ApiModelProperty("插件版本号", required = true)
    val version: String
)
