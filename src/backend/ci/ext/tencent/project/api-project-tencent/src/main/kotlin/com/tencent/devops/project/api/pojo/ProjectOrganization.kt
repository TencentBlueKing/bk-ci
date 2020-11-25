package com.tencent.devops.project.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目组织信息")
data class ProjectOrganization(
    @JsonProperty(value = "project_id", required = true)
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("项目名称")
    @JsonProperty(value = "project_name", required = true)
    val projectName: String,
    @JsonProperty(value = "english_name", required = true)
    @ApiModelProperty("项目英文简称")
    val projectEnglishName: String,
    @JsonProperty(value = "bg_id", required = true)
    @ApiModelProperty("项目所属一级机构ID")
    val bgId: Long,
    @JsonProperty(value = "bg_name", required = true)
    @ApiModelProperty("项目所属一级机构名称")
    val bgName: String,
    @JsonProperty(value = "dept_id", required = true)
    @ApiModelProperty("项目所属二级机构ID")
    val deptId: Long,
    @JsonProperty(value = "dept_name", required = true)
    @ApiModelProperty("项目所属二级机构名称")
    val deptName: String,
    @JsonProperty(value = "center_id", required = true)
    @ApiModelProperty("项目所属三级机构ID")
    val centerId: Long,
    @JsonProperty(value = "center_name", required = true)
    @ApiModelProperty("项目所属三级机构名称")
    val centerName: String,
    @JsonProperty(value = "project_type", required = false)
    @ApiModelProperty("项目类型")
    val projectType: Int?
)