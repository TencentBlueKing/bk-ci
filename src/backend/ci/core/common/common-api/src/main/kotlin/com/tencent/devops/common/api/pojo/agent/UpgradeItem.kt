package com.tencent.devops.common.api.pojo.agent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("需要升级的项")
data class UpgradeItem(
    @ApiModelProperty("升级go agent")
    val agent: Boolean,
    @ApiModelProperty("升级worker")
    val worker: Boolean,
    @ApiModelProperty("升级jdk")
    val jdk: Boolean,
    @ApiModelProperty("升级docker init 脚本")
    val dockerInitFile: Boolean
)
