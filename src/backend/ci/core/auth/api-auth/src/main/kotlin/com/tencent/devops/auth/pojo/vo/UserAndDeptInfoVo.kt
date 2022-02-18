package com.tencent.devops.auth.pojo.vo

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户和组织信息返回")
data class UserAndDeptInfoVo(
    @ApiModelProperty("id")
    val id: Int,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("信息类型")
    val type: ManagerScopesEnum,
    @ApiModelProperty("是否拥有子级")
    val hasChild: Boolean? = false
)
