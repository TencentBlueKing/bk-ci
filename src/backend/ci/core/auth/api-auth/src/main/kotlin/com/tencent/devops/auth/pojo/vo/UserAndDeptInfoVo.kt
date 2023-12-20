package com.tencent.devops.auth.pojo.vo

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.BkUserExtras
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户和组织信息返回实体")
data class UserAndDeptInfoVo(
    @ApiModelProperty("id")
    val id: Int,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("信息类型")
    val type: ManagerScopesEnum,
    @ApiModelProperty("是否拥有子级")
    val hasChild: Boolean? = false,
    @ApiModelProperty("用户部门详细信息")
    val deptInfo: List<BkUserDeptInfo>? = null,
    @ApiModelProperty("用户额外详细信息")
    val extras: BkUserExtras? = null,
    @ApiModelProperty("水印信息")
    val waterMark: String? = null,
    @ApiModelProperty("是否是项目成员")
    val belongProjectMember: Boolean? = null
)
