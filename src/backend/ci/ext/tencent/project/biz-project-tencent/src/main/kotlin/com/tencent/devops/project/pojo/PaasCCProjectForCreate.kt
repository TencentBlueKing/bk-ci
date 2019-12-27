package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModelProperty

data class PaasCCProjectForCreate(
    @ApiModelProperty("项目名称")
    val project_name: String,
    @ApiModelProperty("英文缩写")
    val english_name: String,
    @ApiModelProperty("项目类型")
    val project_type: Int,
    @ApiModelProperty("描述")
    val description: String,
    @ApiModelProperty("事业群ID")
    val bg_id: Long,
    @ApiModelProperty("事业群名字")
    val bg_name: String,
    @ApiModelProperty("部门ID")
    val dept_id: Long,
    @ApiModelProperty("部门名称")
    val dept_name: String,
    @ApiModelProperty("中心ID")
    val center_id: Long,
    @ApiModelProperty("中心名称")
    val center_name: String,
    @ApiModelProperty("是否保密")
    val is_secrecy: Boolean,
    @ApiModelProperty("kind")
    val kind: Int,
    @ApiModelProperty("项目ID")
    val project_id: String,
    @ApiModelProperty("创建人")
    val creator: String
)