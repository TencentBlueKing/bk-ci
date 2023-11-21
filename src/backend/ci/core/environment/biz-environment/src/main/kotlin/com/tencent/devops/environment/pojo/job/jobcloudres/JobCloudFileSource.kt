package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudFileSource(
    @ApiModelProperty(value = "文件类型", notes = "1-服务器文件，2-本地文件，3-文件源文件")
    @JsonProperty("file_type")
    val fileType: Int,
    @ApiModelProperty(value = "文件路径列表")
    @JsonProperty("file_location")
    val fileLocation: List<String>,
    @ApiModelProperty(value = "文件Hash值，仅本地文件该字段有值")
    @JsonProperty("file_hash")
    val fileHash: String,
    @ApiModelProperty(value = "文件大小，单位为字节，仅本地文件该字段有值")
    @JsonProperty("file_size")
    val fileSize: Int,
    @ApiModelProperty(value = "源文件所在机器")
    val host: JobCloudVariableServer,
    @ApiModelProperty(value = "执行账号ID")
    @JsonProperty("account_id")
    val accountId: Long,
    @ApiModelProperty(value = "执行账号名称")
    @JsonProperty("account_name")
    val accountName: String,
    @ApiModelProperty(value = "第三方文件源ID")
    @JsonProperty("file_source_id")
    val fileSourceId: Long,
    @ApiModelProperty(value = "第三方文件源名称")
    @JsonProperty("file_source_name")
    val fileSourceName: String
)