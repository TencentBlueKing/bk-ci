package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "项目组织")
data class ProjectOrganizationInfo(
    @Schema(name = "事业群ID")
    val bgId: Long?,
    @Schema(name = "事业群名字")
    val bgName: String?,
    @Schema(name = "业务线ID")
    val businessLineId: Long?,
    @Schema(name = "业务线名称")
    val businessLineName: String?,
    @Schema(name = "中心ID")
    val centerId: Long?,
    @Schema(name = "中心名称")
    val centerName: String?,
    @Schema(name = "部门ID")
    val deptId: Long?,
    @Schema(name = "部门名称")
    val deptName: String?,
    @Schema(name = "是否需要更正组织")
    val needFix: Boolean = true
)
