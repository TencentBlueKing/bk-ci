package com.tencent.devops.store.pojo.atom

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件参数的多选项")
data class AtomParamOption(
    @ApiModelProperty("流水线ID", required = true)
    val id: String,
    @ApiModelProperty("流水线ID", required = true)
    val name: String
)
