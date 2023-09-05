package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发日志内容")
data class FileLog(
    @ApiModelProperty(value = "分发模式", notes = "0:上传, 1:下载", required = true)
    val mode: Int,
    @ApiModelProperty(value = "文件源主机IP", required = true)
    private val srcIP: IPInfo,
    @ApiModelProperty(value = "源文件路径", required = true)
    private val srcPath: String,
    @ApiModelProperty(value = "分发目标主机IP", notes = "mode == 1 时有值", required = true)
    private val destIP: IPInfo,
    @ApiModelProperty(value = "目标路径", notes = "mode == 1 时有值", required = true)
    private val destPath: String,
    @ApiModelProperty(value = "任务状态", required = true)
    private val status: Int,
    @ApiModelProperty(value = "文件分发日志内容", required = true)
    private val logContent: String
)