package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtItemDTO(
    @ApiModelProperty("项目ID")
    val serviceItem: ServiceItem,
    @ApiModelProperty("项目子集")
    val childItem: List<ServiceItem>
)