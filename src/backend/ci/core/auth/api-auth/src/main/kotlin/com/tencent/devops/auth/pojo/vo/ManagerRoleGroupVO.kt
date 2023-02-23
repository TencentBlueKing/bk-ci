package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.ManagerRoleGroupInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户组信息返回实体")
data class ManagerRoleGroupVO(
    @ApiModelProperty("用户组数量")
    val count: Int,
    @ApiModelProperty("用户组信息")
    val results: List<ManagerRoleGroupInfo>
)
