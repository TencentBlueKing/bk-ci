package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-构建信息")
data class ArtifactoryInfo(
    @ApiModelProperty("流水线号", required = true)
    val pipelineId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int,
    @ApiModelProperty("包ID", required = true)
    val bundleId: String,
    @ApiModelProperty("产物信息", required = true)
    val fileInfo: FileInfo?,
    @ApiModelProperty("包名", required = true)
    val name: String,
    @ApiModelProperty("包全名", required = true)
    val fullName: String,
    @ApiModelProperty("包大小", required = true)
    val size: Long,
    @ApiModelProperty("添加时间", required = true)
    val modifiedTime: Long,
    @ApiModelProperty("app版本", required = true)
    val appVersion: String? = null,
    @ApiModelProperty("数据来源：0-自然数据 1-补偿数据", required = true)
    val dataForm: Int
)