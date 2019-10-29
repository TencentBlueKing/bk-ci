package com.tencent.devops.store.pojo.common

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("卸载插件原因")
data class UnInstallReason(
    @ApiModelProperty("原因ID")
    val reasonId: String,
    @ApiModelProperty("原因说明")
    val note: String?
)