package com.tencent.devops.common.auth.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("权限交接请求体")
data class PermissionHandoverDTO(
    @ApiModelProperty("交接项目集合")
    val projectList: List<String>,
    @ApiModelProperty("交接用户")
    val handoverFrom: String,
    @ApiModelProperty("授予用户")
    val handoverToList: List<String>,
    @ApiModelProperty("资源类型")
    val resourceType: String,
    @ApiModelProperty("是否交接管理员权限")
    val managerPermission: Boolean
)
