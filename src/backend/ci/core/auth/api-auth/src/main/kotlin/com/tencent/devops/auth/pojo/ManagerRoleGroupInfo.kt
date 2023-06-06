package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户组详细信息")
data class ManagerRoleGroupInfo(
    @ApiModelProperty("用户组id")
    val id: Int,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("描述")
    val description: String,
    @ApiModelProperty("是否是只读用户组")
    val readonly: Boolean,
    @ApiModelProperty("用户组成员user数量")
    val userCount: Int,
    @ApiModelProperty("用户组成员department数量")
    val departmentCount: Int,
    @ApiModelProperty("是否已经加入用户组")
    val joined: Boolean,
    @ApiModelProperty("用户组关联的资源类型")
    val resourceType: String,
    @ApiModelProperty("用户组关联的资源类型名称")
    val resourceTypeName: String,
    @ApiModelProperty("用户组关联的资源实例名称")
    val resourceName: String,
    @ApiModelProperty("用户组关联的资源实例code")
    val resourceCode: String
)
