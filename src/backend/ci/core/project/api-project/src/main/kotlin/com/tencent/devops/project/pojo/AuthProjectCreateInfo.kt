package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "项目-资源创建模型")
data class AuthProjectCreateInfo(
    @Schema(name = "用户id")
    val userId: String,
    @Schema(name = "accessToken")
    val accessToken: String?,
    @Schema(name = "用户部门详细")
    val userDeptDetail: UserDeptDetail?,
    @Schema(name = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @Schema(name = "projectCreateInfo")
    val projectCreateInfo: ProjectCreateInfo,
    @Schema(name = "审批状态")
    val approvalStatus: Int
)
