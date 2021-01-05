package com.tencent.devops.process.pojo.op

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂CI支持的插件")
data class GitCiMarketAtom(
    @ApiModelProperty("记录ID", required = true)
    val id: Int,
    @ApiModelProperty("插件code", required = true)
    val atomCode: String,
    @ApiModelProperty("描述", required = true)
    val desc: String
)
