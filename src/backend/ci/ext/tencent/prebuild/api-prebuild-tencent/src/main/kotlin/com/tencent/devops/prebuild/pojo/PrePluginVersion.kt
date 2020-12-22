package com.tencent.devops.prebuild.pojo

import com.tencent.devops.prebuild.pojo.enums.PreBuildPluginType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("prebuild插件版本")
data class PrePluginVersion(
    @ApiModelProperty("插件版本")
    val version: String,
    @ApiModelProperty("插件更新时间")
    val updateTime: String,
    @ApiModelProperty("插件更新人")
    val modifyUser: String,
    @ApiModelProperty("插件更新内容")
    val desc: String,
    @ApiModelProperty("插件类型")
    val pluginType: PreBuildPluginType
)