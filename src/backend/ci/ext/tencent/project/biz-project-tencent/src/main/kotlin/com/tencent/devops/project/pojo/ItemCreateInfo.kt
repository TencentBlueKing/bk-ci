package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModelProperty

data class ItemCreateInfo(
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
    val inputPath: String,
    @ApiModelProperty("添加人")
    val creator: String
)