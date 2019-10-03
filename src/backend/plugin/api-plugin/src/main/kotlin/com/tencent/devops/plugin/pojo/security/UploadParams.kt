package com.tencent.devops.plugin.pojo.security

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("任务数据-上传文件任务数据")
data class UploadParams(
    @ApiModelProperty(value = "文件路径", required = true)
    val filePath: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建No", required = true)
    val buildNo: Int,
    @ApiModelProperty("原子ID", required = true)
    val elementId: String,
    @ApiModelProperty("执行次数", required = true)
    val executeCount: Int,
    @ApiModelProperty(value = "是否是自定义仓库", required = true)
    val custom: Boolean,
    @ApiModelProperty(value = "执行用户", required = true)
    val userId: String,
    @ApiModelProperty(value = "环境id", required = true)
    val envId: String,
    @ApiModelProperty(value = "文件大小", required = true)
    val fileSize: String,
    @ApiModelProperty(value = "文件md5", required = true)
    val fileMd5: String,
    @ApiModelProperty(value = "app版本号", required = true)
    val appVersion: String,
    @ApiModelProperty(value = "app名称", required = true)
    val appTitle: String,
    @ApiModelProperty(value = "app包名", required = true)
    val packageName: String
)