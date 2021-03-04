package com.tencent.devops.process.pojo.op

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂CI支持的插件,新增接口参数")
data class GitCiMarketAtomReq(
    @ApiModelProperty("插件code", required = true)
    val atomCodeList: List<String>,
    @ApiModelProperty("描述")
    val desc: String?
)
