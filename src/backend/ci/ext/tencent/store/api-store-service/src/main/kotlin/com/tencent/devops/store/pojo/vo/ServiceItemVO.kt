package com.tencent.devops.store.pojo.vo

import com.tencent.devops.store.pojo.enums.HtmlComponentTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务功能点")
data class ServiceItemVO(
    @ApiModelProperty("扩展点ID", required = true)
    val itemId: String,
    @ApiModelProperty("扩展点标识", required = true)
    val itemCode: String,
    @ApiModelProperty("扩展点名称", required = true)
    val itemName: String,
    @ApiModelProperty("扩展点对应的页面路径信息", required = true)
    val htmlPath: String,
    @ApiModelProperty("扩展点对应的前端组件类型", required = true)
    val htmlComponentType: HtmlComponentTypeEnum,
    @ApiModelProperty("扩展点对应的前端入口资源路径", required = true)
    val entryResUrl: String
)