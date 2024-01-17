package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目组织")
data class ProjectOrganizationInfo(
    @Schema(title = "事业群ID")
    val bgId: Long?,
    @Schema(title = "事业群名字")
    val bgName: String?,
    @Schema(title = "业务线ID")
    val businessLineId: Long?,
    @Schema(title = "业务线名称")
    val businessLineName: String?,
    @Schema(title = "中心ID")
    val centerId: Long?,
    @Schema(title = "中心名称")
    val centerName: String?,
    @Schema(title = "部门ID")
    val deptId: Long?,
    @Schema(title = "部门名称")
    val deptName: String?,
    @Schema(title = "是否需要更正组织")
    val needFix: Boolean = true
)
