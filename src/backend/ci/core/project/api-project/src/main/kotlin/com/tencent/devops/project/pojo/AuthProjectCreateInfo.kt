package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目-资源创建模型")
data class AuthProjectCreateInfo(
    @Schema(title = "用户id")
    val userId: String,
    @Schema(title = "accessToken")
    val accessToken: String?,
    @Schema(title = "用户部门详细")
    val userDeptDetail: UserDeptDetail?,
    @Schema(title = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @Schema(title = "projectCreateInfo")
    val projectCreateInfo: ProjectCreateInfo,
    @Schema(title = "审批状态")
    val approvalStatus: Int
)
