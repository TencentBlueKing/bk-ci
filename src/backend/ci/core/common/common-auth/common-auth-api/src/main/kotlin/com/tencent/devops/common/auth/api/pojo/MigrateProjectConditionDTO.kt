package com.tencent.devops.common.auth.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("条件迁移项目实体")
data class MigrateProjectConditionDTO(
    @ApiModelProperty("中心ID")
    val centerId: Long? = null,
    @ApiModelProperty("部门ID")
    val deptId: Long? = null,
    @ApiModelProperty("项目创建人")
    val projectCreator: String? = null,
    @ApiModelProperty("排除项目code")
    val excludedProjectCodes: List<String>? = null,
    @ApiModelProperty("项目ID列表")
    val projectCodes: List<String>? = null,
    @ApiModelProperty("资源类型")
    val resourceType: String? = null
)
