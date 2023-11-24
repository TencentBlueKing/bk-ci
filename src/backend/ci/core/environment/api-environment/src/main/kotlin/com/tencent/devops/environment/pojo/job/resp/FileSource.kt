package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class FileSource(
    @ApiModelProperty(value = "文件类型", notes = "1-服务器文件，2-本地文件，3-文件源文件")
    val fileType: Int,
    @ApiModelProperty(value = "文件路径列表")
    val fileList: List<String>,
    @ApiModelProperty(value = "源文件所在机器")
    val server: VariableServer?,
    @ApiModelProperty(value = "执行账号")
    val account: Account,
    @ApiModelProperty(value = "第三方文件源ID")
    val fileSourceId: Long?
)