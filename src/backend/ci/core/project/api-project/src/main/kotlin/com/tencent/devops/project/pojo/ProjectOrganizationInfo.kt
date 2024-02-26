package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目组织")
data class ProjectOrganizationInfo(
    @get:Schema(title = "事业群ID")
    val bgId: Long?,
    @get:Schema(title = "事业群名字")
    val bgName: String?,
    @get:Schema(title = "业务线ID")
    val businessLineId: Long?,
    @get:Schema(title = "业务线名称")
    val businessLineName: String?,
    @get:Schema(title = "中心ID")
    val centerId: Long?,
    @get:Schema(title = "中心名称")
    val centerName: String?,
    @get:Schema(title = "部门ID")
    val deptId: Long?,
    @get:Schema(title = "部门名称")
    val deptName: String?,
    @get:Schema(title = "是否需要更正组织")
    val needFix: Boolean = true
)
