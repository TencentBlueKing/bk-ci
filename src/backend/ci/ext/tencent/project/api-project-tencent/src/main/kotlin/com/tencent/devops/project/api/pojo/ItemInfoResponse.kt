package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("添加扩展点入参")
data class ItemInfoResponse(
    @ApiModelProperty("扩展点名称")
    val itemName: String,
    @ApiModelProperty("扩展点标示")
    val itemCode: String,
    @ApiModelProperty("扩展服务Id")
    val pid: String,
    @ApiModelProperty("UI组件类型")
    val UIType: String,
    @ApiModelProperty("页面路径")
    val htmlPath: String,
    @ApiModelProperty("入口字段路径")
    val inputPath: String
)