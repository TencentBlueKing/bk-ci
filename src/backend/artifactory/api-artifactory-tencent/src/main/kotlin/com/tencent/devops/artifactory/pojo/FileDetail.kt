package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件详细信息")
data class FileDetail(
    @ApiModelProperty("文件名", required = true)
    val name: String,
    @ApiModelProperty("文件路径", required = true)
    val path: String,
    @ApiModelProperty("文件全名", required = true)
    val fullName: String,
    @ApiModelProperty("文件全路径", required = true)
    val fullPath: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("创建时间", required = true)
    val createdTime: Long,
    @ApiModelProperty("修改时间", required = true)
    val modifiedTime: Long,
    @ApiModelProperty("文件摘要", required = true)
    val checksums: FileChecksums,
    @ApiModelProperty("meta数据", required = true)
    val meta: Map<String, String>
)