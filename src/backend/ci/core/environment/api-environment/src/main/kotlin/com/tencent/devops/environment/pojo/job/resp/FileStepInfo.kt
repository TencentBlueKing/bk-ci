package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class FileStepInfo(
    @ApiModelProperty(value = "源文件列表")
    val fileSourceList: List<FileSource>
)