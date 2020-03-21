package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("作业平台分发对象")
data class FastPushFileRequest(
    @ApiModelProperty("操作人ID")
    val userId: String,
    @ApiModelProperty("文件信息")
    val fileSources: List<FileSource>,
    @ApiModelProperty("目标机器路径")
    val fileTargetPath: String,
    @ApiModelProperty("目标机器信息")
    val envSet: EnvSet,
    @ApiModelProperty("目标机器操作用户")
    val account: String,
    @ApiModelProperty("超时时间")
    val timeout: Long?
) {
    data class FileSource(
        val files: List<String>,
        val envSet: EnvSet,
        val account: String
    )
}
