package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "文件分发日志内容")
data class FileLog(
    @get:Schema(title = "分发模式", description = "0:上传, 1:下载", required = true)
    val mode: Int,
    @get:Schema(title = "文件源主机信息", required = true)
    val srcHost: HostInRes,
    @get:Schema(title = "源文件路径", required = true)
    val srcPath: String,
    @get:Schema(title = "分发目标主机信息", description = "mode == 1 时有值")
    val destHost: HostInRes? = null,
    @get:Schema(title = "目标路径", description = "mode == 1 时有值")
    val destPath: String?,
    @get:Schema(title = "任务状态", description = "1-等待开始，2-上传中，3-下载中，4-成功，5-失败", required = true)
    val status: Int,
    @get:Schema(title = "文件分发日志内容", required = true)
    val logContent: String,
    @get:Schema(title = "文件大小")
    val size: String?,
    @get:Schema(title = "文件传输速率")
    val speed: String?,
    @get:Schema(title = "文件传输进度")
    val process: String?
)