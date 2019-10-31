package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾工蜂项目配置")
data class GitProjectPipeline(
    @ApiModelProperty("工蜂项目ID")
    val gitProjectId: Long,
    @ApiModelProperty("蓝盾项目Code")
    val projectCode: String,
    @ApiModelProperty("蓝盾流水线ID")
    val pipelineId: String,
    @ApiModelProperty("创建时间")
    val createTime: Long?,
    @ApiModelProperty("修改时间")
    val updateTime: Long?
)
