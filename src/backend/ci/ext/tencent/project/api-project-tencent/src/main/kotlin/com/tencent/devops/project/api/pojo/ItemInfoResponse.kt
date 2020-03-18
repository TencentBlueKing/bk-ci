package com.tencent.devops.project.api.pojo

import com.tencent.devops.project.api.pojo.enums.HtmlComponentTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展点操作入参")
data class ItemInfoResponse(
    @ApiModelProperty("扩展点名称")
    val itemName: String,
    @ApiModelProperty("扩展点标示")
    val itemCode: String,
    @ApiModelProperty("扩展服务Id（父级）")
    val pid: String,
    @ApiModelProperty("UI组件类型")
    val UiType: HtmlComponentTypeEnum,
    @ApiModelProperty("页面路径")
    val htmlPath: String,
    @ApiModelProperty("icon地址")
    val iconUrl: String?,
    @ApiModelProperty("提示信息")
    val tooltip: String?,
    @ApiModelProperty("自定义扩展点前端表单属性配置Json串")
    val props: String?
)