package com.tencent.devops.plugin.pojo.wetest

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest云设备信息")
data class WetestCloud(
    @ApiModelProperty("id")
    @JsonProperty("id")
    val id: Int,
    @ApiModelProperty("name")
    @JsonProperty("name")
    val name: String,
    @ApiModelProperty("devices")
    @JsonProperty("devices")
    val devices: List<WetestDevice>
)
