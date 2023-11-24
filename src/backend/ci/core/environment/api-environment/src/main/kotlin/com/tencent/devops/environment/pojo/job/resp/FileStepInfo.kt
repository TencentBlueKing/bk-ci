package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class FileStepInfo(
    @ApiModelProperty(value = "源文件列表")
    val fileSourceList: List<FileSource>,
    @ApiModelProperty(value = "目标信息")
    val fileDestination: FileDestination,
    @ApiModelProperty(value = "超时时间", notes = "单位为秒")
    val timeout: Int,
    @ApiModelProperty(value = "上传文件限速", notes = "单位为MB/s，没有值表示不限速")
    val sourceSpeedLimit: Int?,
    @ApiModelProperty(value = "下载文件限速", notes = "单位为MB/s，没有值表示不限速")
    val destinationSpeedLimit: Int?,
    @ApiModelProperty(value = "传输模式", notes = "1 - 严谨模式, 2 - 强制模式, 3 - 安全模式")
    val transferMode: Int,
    @ApiModelProperty(value = "是否忽略错误", notes = "0-不忽略，1-忽略")
    val isIgnoreError: Int
)