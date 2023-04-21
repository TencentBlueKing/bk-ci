package com.tencent.devops.project.pojo

import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-资源修改模型")
data class ResourceUpdateInfo(
    @ApiModelProperty("用户id")
    val userId: String,
    @ApiModelProperty("前端传递的项目修改信息")
    val projectUpdateInfo: ProjectUpdateInfo,
    /*@ApiModelProperty("数据库获取的项目信息")
    val projectInfo: ProjectVO,*/
    @ApiModelProperty("是否需要审批（从页面调起）")
    val needApproval: Boolean,
    @ApiModelProperty("iamSubjectScopes")
    val subjectScopes: List<SubjectScopeInfo>,
    @ApiModelProperty("审批状态")
    val approvalStatus: Int
)
