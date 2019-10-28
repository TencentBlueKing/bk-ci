package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目配置(分页)")
data class ProjectConfigPage(
    @ApiModelProperty("projectConfig总数", required = true)
    val total: Int,
    @ApiModelProperty("projectConfig详情", required = true)
    val data: List<ProjectConfig>
)