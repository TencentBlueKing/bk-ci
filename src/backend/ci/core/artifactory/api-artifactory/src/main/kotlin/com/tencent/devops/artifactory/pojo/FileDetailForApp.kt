package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
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
    val creator: String,
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String,
    @ApiModelProperty("logo链接", required = false)
    val logoUrl: String,
    @ApiModelProperty("文件路径", required = true)
    val path: String,
    @ApiModelProperty("文件全名", required = true)
    val fullName: String,
    @ApiModelProperty("文件全路径", required = true)
    val fullPath: String,
    @ApiModelProperty("仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("修改时间", required = true)
    val modifiedTime: Long,
    @ApiModelProperty("md5", required = true)
    val md5: String
)
