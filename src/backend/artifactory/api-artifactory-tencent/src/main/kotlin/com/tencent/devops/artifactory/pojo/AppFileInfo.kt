package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件信息")
data class AppFileInfo(
    @ApiModelProperty("文件名", required = true)
    val name: String,
    @ApiModelProperty("文件全名", required = true)
    val fullName: String,
    @ApiModelProperty("文件路径", required = true)
    val path: String,
    @ApiModelProperty("文件全路径", required = true)
    val fullPath: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("是否文件夹", required = true)
    val folder: Boolean,
    @ApiModelProperty("更新时间", required = true)
    val modifiedTime: Long,
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("是否显示", required = true)
    val show: Boolean,
    @ApiModelProperty("是否可下载", required = true)
    val canDownload: Boolean,
    @ApiModelProperty("版本信息", required = true)
    val version: String? = null
)