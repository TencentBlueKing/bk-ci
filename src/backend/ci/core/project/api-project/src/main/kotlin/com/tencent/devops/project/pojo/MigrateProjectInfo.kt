package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.enums.AuthSystemType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("迁移项目详细信息")
data class MigrateProjectInfo(
    @ApiModelProperty("项目英文名称")
    val englishName: String,
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("权限系统版本")
    val authSystemType: String?,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("创建人是否离职")
    val creatorNotExist: Boolean
)
