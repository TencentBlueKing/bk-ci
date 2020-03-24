package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class ProjectEnableInfo(
    @ApiModelProperty("项目名称")
    @JsonProperty("project_name")
    val projectName: String,
    @ApiModelProperty("英文缩写")
    @JsonProperty("english_name")
    val englishName: String,
    @ApiModelProperty("启用")
    val enabled: Boolean?
)