package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DevCloud容器机型")
data class DevCloudModel(
    @ApiModelProperty("moduleId", required = true)
    val moduleId: String,
    @ApiModelProperty("机型名称", required = true)
    val moduleName: String,
    @ApiModelProperty("CPU", required = true)
    val cpu: Int,
    @ApiModelProperty("Memory", required = true)
    val memory: String,
    @ApiModelProperty("Disk", required = true)
    val disk: String,
    @ApiModelProperty("description", required = true)
    val description: List<String>?,
    @ApiModelProperty("description", required = true)
    val produceTime: String?
)