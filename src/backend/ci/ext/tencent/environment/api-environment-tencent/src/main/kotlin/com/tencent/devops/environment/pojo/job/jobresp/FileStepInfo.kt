package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class FileStepInfo(
    @get:Schema(title = "源文件列表")
    val fileSourceList: List<FileSource>,
    @get:Schema(title = "目标信息")
    val fileDestination: FileDestination,
    @get:Schema(title = "超时时间", description = "单位为秒")
    val timeout: Int,
    @get:Schema(title = "上传文件限速", description = "单位为MB/s，没有值表示不限速")
    val sourceSpeedLimit: Int?,
    @get:Schema(title = "下载文件限速", description = "单位为MB/s，没有值表示不限速")
    val destinationSpeedLimit: Int?,
    @get:Schema(title = "传输模式", description = "1 - 严谨模式, 2 - 强制模式, 3 - 安全模式")
    val transferMode: Int,
    @get:Schema(title = "是否忽略错误", description = "0-不忽略，1-忽略")
    val isIgnoreError: Int
)