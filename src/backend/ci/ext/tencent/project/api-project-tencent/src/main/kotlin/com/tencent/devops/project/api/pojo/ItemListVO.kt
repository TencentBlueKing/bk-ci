package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("查询扩展点请求对象")
class ItemListVO (
    @ApiModelProperty("总记录数", required = true)
    val count: Int,
    @ApiModelProperty("当前页码值", required = false)
    val page: Int?,
    @ApiModelProperty("每页记录大小", required = false)
    val pageSize: Int?,
    @ApiModelProperty("每页记录大小", required = false)
    val itemList: List<ServiceItem>
)