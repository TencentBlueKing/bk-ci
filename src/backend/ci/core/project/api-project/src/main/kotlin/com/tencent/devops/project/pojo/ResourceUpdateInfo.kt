package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目-资源修改模型")
data class ResourceUpdateInfo(
    @Schema(description = "用户id")
    val userId: String,
    @Schema(description = "前端传递的项目修改信息")
    val projectUpdateInfo: ProjectUpdateInfo,
    /*@Schema(description = "数据库获取的项目信息")
    val projectInfo: ProjectVO,*/
    @Schema(description = "是否需要审批（从页面调起）")
    val needApproval: Boolean,
    @Schema(description = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @Schema(description = "审批状态")
    val approvalStatus: Int
)
