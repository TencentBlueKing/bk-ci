package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class PaasCCProjectInfo(
    @ApiModelProperty("项目名称")
    @JsonProperty("project_name")
    val projectName: String,
    @ApiModelProperty("英文缩写")
    @JsonProperty("english_name")
    val englishName: String,
    @ApiModelProperty("项目类型")
    @JsonProperty("project_type")
    val projectType: Int,
    @ApiModelProperty("描述")
    val description: String,
    @ApiModelProperty("一级部门ID")
    @JsonProperty("bg_id")
    val bgId: Long,
    @ApiModelProperty("一级部门名字")
    @JsonProperty("bg_name")
    val bgName: String,
    @ApiModelProperty("二级部门ID")
    @JsonProperty("dept_id")
    val deptId: Long,
    @ApiModelProperty("二级部门名称")
    @JsonProperty("dept_name")
    val deptName: String,
    @ApiModelProperty("三级部门ID")
    @JsonProperty("center_id")
    val centerId: Long,
    @ApiModelProperty("三级部门名称")
    @JsonProperty("center_name")
    val centerName: String,
    @ApiModelProperty("是否保密")
    @get:JsonProperty("is_secrecy")
    var secrecy: Boolean = false,
    @ApiModelProperty("kind")
    val kind: Int = 0
)