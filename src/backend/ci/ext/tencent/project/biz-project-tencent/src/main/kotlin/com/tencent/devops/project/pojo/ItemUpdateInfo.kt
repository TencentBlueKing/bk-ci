package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModelProperty

data class ItemUpdateInfo(
    @ApiModelProperty("扩展点名称")
    val itemName: String?,
    @ApiModelProperty("父级Id")
    val pid: String?,
    @ApiModelProperty("UI组件类型")
    val UIType: String?,
    @ApiModelProperty("页面路径")
    val htmlPath: String?,
    @ApiModelProperty("入口字段路径")
    val inputPath: String?,
    @ApiModelProperty("icon地址")
    val iconUrl: String?,
    @ApiModelProperty("提示信息")
    val tooltip: String?,
    @ApiModelProperty("自定义扩展点前端表单属性配置Json串")
    val props: String?
)