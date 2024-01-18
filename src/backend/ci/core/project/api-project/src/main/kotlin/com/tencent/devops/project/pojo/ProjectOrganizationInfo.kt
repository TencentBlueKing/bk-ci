package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目组织")
data class ProjectOrganizationInfo(
    @ApiModelProperty("事业群ID")
    val bgId: Long?,
    @ApiModelProperty("事业群名字")
    val bgName: String?,
    @ApiModelProperty("业务线ID")
    val businessLineId: Long?,
    @ApiModelProperty("业务线名称")
    val businessLineName: String?,
    @ApiModelProperty("中心ID")
    val centerId: Long?,
    @ApiModelProperty("中心名称")
    val centerName: String?,
    @ApiModelProperty("部门ID")
    val deptId: Long?,
    @ApiModelProperty("部门名称")
    val deptName: String?,
    @ApiModelProperty("是否需要更正组织")
    val needFix: Boolean = true
)
