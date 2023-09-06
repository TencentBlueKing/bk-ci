package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发日志内容")
data class FileLog(
    @ApiModelProperty(value = "分发模式", notes = "0:上传, 1:下载", required = true)
    val mode: Int,
    @ApiModelProperty(value = "文件源主机IP", required = true)
    val srcIP: HostInfo,
    @ApiModelProperty(value = "源文件路径", required = true)
    val srcPath: String,
    @ApiModelProperty(value = "分发目标主机IP", notes = "mode == 1 时有值", required = true)
    val destIP: HostInfo,
    @ApiModelProperty(value = "目标路径", notes = "mode == 1 时有值", required = true)
    val destPath: String,
    @ApiModelProperty(value = "任务状态", notes = "1-等待开始，2-上传中，3-下载中，4-成功，5-失败", required = true)
    val status: Int,
    @ApiModelProperty(value = "文件分发日志内容", required = true)
    val logContent: String
)