package com.tencent.devops.common.api.pojo.log

import com.tencent.devops.common.api.enums.log.LogStorageMode
import io.swagger.v3.oas.annotations.media.Schema
import java.io.File

data class TaskBuildLogProperty(
    @get:Schema(title = "插件任务ID", required = true)
    val elementId: String,
    @get:Schema(title = "日志文件子路径", required = true)
    val childPath: String,
    @get:Schema(title = "日志zip文件子路径", required = true)
    val childZipPath: String?,
    @get:Schema(title = "日志文件句柄", required = true)
    val logFile: File,
    @get:Schema(title = "日志的存储模式", required = false)
    var logStorageMode: LogStorageMode = LogStorageMode.UPLOAD
)
