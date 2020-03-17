package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModelProperty

data class ServiceItem(
    @ApiModelProperty("扩展Id")
    val itemId: String,
    @ApiModelProperty("扩展名称")
    val itemName: String,
    @ApiModelProperty("扩展标示编码")
    val itemCode: String,
    @ApiModelProperty("扩展蓝盾服务Id")
    val parentId: String,
    @ApiModelProperty("扩展蓝盾服务Name")
    var parentName: String? = "",
    @ApiModelProperty("页面路径")
    val htmlPath: String? = null,
    @ApiModelProperty("UI组件类型")
    val htmlType: String? = null,
    @ApiModelProperty("扩展服务安装个数")
    val serviceCount: Int? = 0,
    val tooltip: String? = "",
    @ApiModelProperty("icon路径")
    val icon: String? = "",
    @ApiModelProperty("props参数")
    val props: String = ""
)