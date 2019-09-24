package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-构建信息")
data class ArtifactoryCreateInfo(
    @ApiModelProperty("流水线号", required = true)
    val pipelineId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int,
    @ApiModelProperty("产物信息", required = true)
    val fileInfo: FileInfo?,
    @ApiModelProperty("数据来源：0-自然数据 1-补偿数据", required = true)
    val dataForm: Int,
    @ApiModelProperty("添加时间", required = true)
    val modifiedTime: Long
)