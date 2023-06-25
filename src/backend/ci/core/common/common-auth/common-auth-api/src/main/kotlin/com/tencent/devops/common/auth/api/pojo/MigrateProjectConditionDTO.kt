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
    @ApiModelProperty("迁移项目Code--可包含已迁移的项目")
    val migrateProjectCodes: List<String>? = null,
    @ApiModelProperty("迁移的资源类型")
    val migrateResourceType: String? = null,
    @ApiModelProperty("排除项目code")
    val excludedProjectCodes: List<String>? = null
)
