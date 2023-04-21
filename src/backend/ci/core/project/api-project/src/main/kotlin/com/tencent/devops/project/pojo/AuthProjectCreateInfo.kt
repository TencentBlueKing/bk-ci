package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-资源创建模型")
data class AuthProjectCreateInfo(
    @ApiModelProperty("用户id")
    val userId: String,
    @ApiModelProperty("accessToken")
    val accessToken: String?,
    @ApiModelProperty("用户部门详细")
    val userDeptDetail: UserDeptDetail?,
    @ApiModelProperty("iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @ApiModelProperty("projectCreateInfo")
    val projectCreateInfo: ProjectCreateInfo,
    @ApiModelProperty("审批状态")
    val approvalStatus: Int
)
