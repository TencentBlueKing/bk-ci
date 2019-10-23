package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建模型-ID")
data class ProjectBuildId(
    @ApiModelProperty("构建ID", required = true)
    val id: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String
)