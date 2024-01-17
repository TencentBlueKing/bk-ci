package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目-资源修改模型")
data class ResourceUpdateInfo(
    @Schema(title = "用户id")
    val userId: String,
    @Schema(title = "前端传递的项目修改信息")
    val projectUpdateInfo: ProjectUpdateInfo,
    /*@Schema(title = "数据库获取的项目信息")
    val projectInfo: ProjectVO,*/
    @Schema(title = "是否需要审批（从页面调起）")
    val needApproval: Boolean,
    @Schema(title = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @Schema(title = "审批状态")
    val approvalStatus: Int
)
