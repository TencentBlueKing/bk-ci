package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("文件分发日志内容")
data class JobCloudFileLog(
    @ApiModelProperty(value = "分发模式", notes = "0:上传, 1:下载", required = true)
    val mode: Int,
    @ApiModelProperty(value = "文件源主机信息", required = true)
    @JsonProperty("src_ip")
    val srcHost: JobCloudHostInRes,
    @ApiModelProperty(value = "源文件路径", required = true)
    @JsonProperty("src_path")
    val srcPath: String,
    @ApiModelProperty(value = "分发目标主机信息", notes = "mode == 1 时有值")
    @JsonProperty("dest_ip")
    val destHost: JobCloudHostInRes? = null,
    @ApiModelProperty(value = "目标路径", notes = "mode == 1 时有值")
    @JsonProperty("dest_path")
    val destPath: String?,
    @ApiModelProperty(value = "任务状态", notes = "1-等待开始，2-上传中，3-下载中，4-成功，5-失败", required = true)
    val status: Int,
    @ApiModelProperty(value = "文件分发日志内容", required = true)
    @JsonProperty("log_content")
    val logContent: String,
    @ApiModelProperty(value = "文件大小")
    val size: String?,
    @ApiModelProperty(value = "文件传输速率")
    val speed: String?,
    @ApiModelProperty(value = "文件传输进度")
    val process: String?
)