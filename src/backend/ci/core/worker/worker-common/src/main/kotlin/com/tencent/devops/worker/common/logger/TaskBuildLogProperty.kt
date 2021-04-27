package com.tencent.devops.worker.common.logger

import com.tencent.devops.worker.common.env.LogMode
import io.swagger.annotations.ApiModelProperty
import java.io.File

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 16:31 2021/4/27
 */

data class TaskBuildLogProperty(
    @ApiModelProperty("插件任务ID", required = true)
    val elementId: String,
    @ApiModelProperty("日志文件子路径", required = true)
    val childPath: String,
    @ApiModelProperty("日志文件句柄", required = true)
    val logFile: File,
    @ApiModelProperty("日志的存储模式", required = false)
    var logMode: LogMode = LogMode.UPLOAD
)
