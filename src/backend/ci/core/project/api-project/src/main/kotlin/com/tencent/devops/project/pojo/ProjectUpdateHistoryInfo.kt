package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目更新历史记录实体")
data class ProjectUpdateHistoryInfo(
    @get:Schema(title = "英文名称")
    val englishName: String,
    @get:Schema(title = "变更前项目名称")
    val beforeProjectName: String,
    @get:Schema(title = "变更后项目名称")
    val afterProjectName: String,
    @get:Schema(title = "变更前运营产品ID")
    val beforeProductId: Int?,
    @get:Schema(title = "变更后运营产品ID")
    val afterProductId: Int?,
    @get:Schema(title = "变更前组织架构")
    val beforeOrganization: String,
    @get:Schema(title = "变更后组织架构")
    val afterOrganization: String,
    @get:Schema(title = "变更前最大可授权人员范围")
    val beforeSubjectScopes: String,
    @get:Schema(title = "变更后最大可授权人员范围")
    val afterSubjectScopes: String,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "审批状态")
    val approvalStatus: Int
)
