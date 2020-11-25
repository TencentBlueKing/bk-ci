package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class PaasCCProjectForUpdate(
    @ApiModelProperty("项目名称")
    val project_name: String,
    @ApiModelProperty("项目名称")
    val project_code: String,
    @ApiModelProperty("项目类型")
    val project_type: Int,
    @ApiModelProperty("事业群ID")
    val bg_id: Long,
    @ApiModelProperty("事业群名字")
    val bg_name: String,
    @ApiModelProperty("中心ID")
    val center_id: Long,
    @ApiModelProperty("中心名称")
    val center_name: String,
    @ApiModelProperty("部门ID")
    val dept_id: Long,
    @ApiModelProperty("部门名称")
    val dept_name: String,
    @ApiModelProperty("描述")
    val description: String,
    @ApiModelProperty("英文缩写")
    val english_name: String,
    @ApiModelProperty("修改人")
    val updator: String,
    @ApiModelProperty("应用ID")
    val cc_app_id: Long?,
    @ApiModelProperty("名称")
    val cc_app_name: String?,
    @ApiModelProperty("容器类型， 1 - k8s; 2 - mesos")
    val kind: Int?,
    @ApiModelProperty("保密性")
    @JsonProperty("is_secrecy")
    val secrecy: Boolean
)