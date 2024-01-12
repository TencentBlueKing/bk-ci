package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目组织")
data class ProjectOrganizationInfo(
    @Schema(description = "事业群ID")
    val bgId: Long?,
    @Schema(description = "事业群名字")
    val bgName: String?,
    @Schema(description = "业务线ID")
    val businessLineId: Long?,
    @Schema(description = "业务线名称")
    val businessLineName: String?,
    @Schema(description = "中心ID")
    val centerId: Long?,
    @Schema(description = "中心名称")
    val centerName: String?,
    @Schema(description = "部门ID")
    val deptId: Long?,
    @Schema(description = "部门名称")
    val deptName: String?,
    @Schema(description = "是否需要更正组织")
    val needFix: Boolean = true
)
