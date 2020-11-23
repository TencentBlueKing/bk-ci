package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件详细信息")
data class FileDetailForApp(
    @ApiModelProperty("文件名", required = true)
    val name: String,
    @ApiModelProperty("平台", required = true)
    val platform: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("创建时间", required = true)
    val createdTime: Long,
    @ApiModelProperty("项目", required = true)
    val projectName: String,
    @ApiModelProperty("流水线", required = true)
    val pipelineName: String,
    @ApiModelProperty("执行人", required = true)
    val creator: String
)
