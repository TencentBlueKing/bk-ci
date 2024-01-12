package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目-资源创建模型")
data class AuthProjectCreateInfo(
    @Schema(description = "用户id")
    val userId: String,
    @Schema(description = "accessToken")
    val accessToken: String?,
    @Schema(description = "用户部门详细")
    val userDeptDetail: UserDeptDetail?,
    @Schema(description = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @Schema(description = "projectCreateInfo")
    val projectCreateInfo: ProjectCreateInfo,
    @Schema(description = "审批状态")
    val approvalStatus: Int
)
