package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtItemDTO(
    @ApiModelProperty("")
    val extServiceItem: ExtServiceEntity,
    @ApiModelProperty("项目子集")
    val childItem: List<ExtServiceEntity>
)