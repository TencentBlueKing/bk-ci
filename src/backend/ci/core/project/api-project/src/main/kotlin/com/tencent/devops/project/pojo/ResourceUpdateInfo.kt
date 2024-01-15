package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "项目-资源修改模型")
data class ResourceUpdateInfo(
    @Schema(name = "用户id")
    val userId: String,
    @Schema(name = "前端传递的项目修改信息")
    val projectUpdateInfo: ProjectUpdateInfo,
    /*@Schema(name = "数据库获取的项目信息")
    val projectInfo: ProjectVO,*/
    @Schema(name = "是否需要审批（从页面调起）")
    val needApproval: Boolean,
    @Schema(name = "iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @Schema(name = "审批状态")
    val approvalStatus: Int
)
