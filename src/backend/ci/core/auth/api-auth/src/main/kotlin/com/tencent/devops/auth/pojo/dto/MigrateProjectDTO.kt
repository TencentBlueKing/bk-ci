package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class MigrateProjectDTO(
    @ApiModelProperty("指定审批人", required = false)
    val approver: String?,
    @ApiModelProperty("项目Code", required = false)
    val projectCode: String
)
