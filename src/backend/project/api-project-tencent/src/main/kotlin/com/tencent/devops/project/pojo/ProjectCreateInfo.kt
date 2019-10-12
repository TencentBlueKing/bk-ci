package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-新增模型")
data class ProjectCreateInfo(
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("英文缩写")
    val englishName: String,
    @ApiModelProperty("项目类型")
    val projectType: Int,
    @ApiModelProperty("描述")
    val description: String,
    @ApiModelProperty("事业群ID")
    val bgId: Long,
    @ApiModelProperty("事业群名字")
    val bgName: String,
    @ApiModelProperty("部门ID")
    val deptId: Long,
    @ApiModelProperty("部门名称")
    val deptName: String,
    @ApiModelProperty("中心ID")
    val centerId: Long,
    @ApiModelProperty("中心名称")
    val centerName: String,
    @ApiModelProperty("是否保密")
    val isSecrecy: Boolean,
    @ApiModelProperty("kind")
    val kind: Int
)