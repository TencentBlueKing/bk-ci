package com.tencent.devops.environment.pojo.job.resp

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class FileSource(
    @ApiModelProperty(value = "文件类型", notes = "1-服务器文件，2-本地文件，3-文件源文件")
    val fileType: Int,
    @ApiModelProperty(value = "文件路径列表")
    val fileLocation: List<String>,
    @ApiModelProperty(value = "文件Hash值，仅本地文件该字段有值")
    val fileHash: String,
    @ApiModelProperty(value = "文件大小，单位为字节，仅本地文件该字段有值")
    val fileSize: Int,
    @ApiModelProperty(value = "源文件所在机器")
    val host: VariableServer,
    @ApiModelProperty(value = "执行账号ID")
    val accountId: Long,
    @ApiModelProperty(value = "执行账号名称")
    val accountName: String,
    @ApiModelProperty(value = "第三方文件源ID")
    val fileSourceId: Long,
    @ApiModelProperty(value = "第三方文件源名称")
    val fileSourceName: String
)