package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("OBS基礎字典数据DTO")
data class ObsBaseDictDTO(
    @ApiModelProperty("jsonrpc")
    val jsonrpc: String,
    @ApiModelProperty("id")
    val id: String,
    @ApiModelProperty("method")
    val method: String,
    @ApiModelProperty("params")
    val params: Map<String, String>
)
