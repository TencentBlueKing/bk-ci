package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("按条件迁移项目实体")
data class MigrateProjectConditionDTO(
    @ApiModelProperty("中心名称")
    val centerName: String? = null,
    @ApiModelProperty("部门名称")
    val deptName: String? = null,
    @ApiModelProperty("不迁移项目Code")
    val excludedProjectCodes: List<String>? = null
)
