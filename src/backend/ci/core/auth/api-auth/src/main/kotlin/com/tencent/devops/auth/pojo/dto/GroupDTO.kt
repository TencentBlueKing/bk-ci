package com.tencent.devops.auth.pojo.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
data class GroupDTO(
    @ApiModelProperty("用户组编号, 内置用户组编号固定, 自定义组动态生成")
    val groupCode: String,
    @ApiModelProperty("用户组分类")
    val groupType: Int,
    @ApiModelProperty("用户组名称")
    val groupName: String,
    @ApiModelProperty("权限集合")
    val authPermissionList: List<String>?
)