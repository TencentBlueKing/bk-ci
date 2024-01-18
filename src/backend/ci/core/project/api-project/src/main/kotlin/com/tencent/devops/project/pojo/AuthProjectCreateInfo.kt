package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目-资源创建模型")
data class AuthProjectCreateInfo(
    @get:Schema(title = "用户id")
    val userId: String,
    @get:Schema(title = "accessToken")
    val accessToken: String?,
    @get:Schema(title = "用户部门详细")
    val userDeptDetail: UserDeptDetail?,
    @get:Schema(title = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @get:Schema(title = "projectCreateInfo")
    val projectCreateInfo: ProjectCreateInfo,
    @get:Schema(title = "审批状态")
    val approvalStatus: Int
)
