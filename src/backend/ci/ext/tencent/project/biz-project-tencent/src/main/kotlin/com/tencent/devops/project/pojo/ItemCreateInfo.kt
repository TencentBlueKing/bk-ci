package com.tencent.devops.project.pojo

import com.tencent.devops.project.api.pojo.enums.HtmlComponentTypeEnum
import io.swagger.annotations.ApiModelProperty

data class ItemCreateInfo(
    @ApiModelProperty("扩展点名称")
    val itemName: String,
    @ApiModelProperty("扩展点标示")
    val itemCode: String,
    @ApiModelProperty("蓝盾服务ID")
    val serviceId: String,
    @ApiModelProperty("UI组件类型")
    val UIType: HtmlComponentTypeEnum,
    @ApiModelProperty("页面路径")
    val htmlPath: String,
    @ApiModelProperty("icon地址")
    val iconUrl: String?,
    @ApiModelProperty("提示信息")
    val tooltip: String?,
    @ApiModelProperty("自定义扩展点前端表单属性配置Json串")
    val props: String?,
    @ApiModelProperty("添加人")
    val creator: String
)