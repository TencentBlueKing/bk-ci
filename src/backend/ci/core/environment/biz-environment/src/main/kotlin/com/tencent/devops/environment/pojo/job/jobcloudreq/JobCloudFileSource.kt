package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
class JobCloudFileSource(
    @ApiModelProperty(value = "文件列表", required = true)
    @JsonProperty("file_list")
    val fileList: List<String>,
    @ApiModelProperty(value = "源文件服务器", required = true)
    @JsonProperty("server")
    val server: JobCloudExecuteTarget,
    @ApiModelProperty(value = "文件源账号", required = true)
    @JsonProperty("account")
    val account: JobCloudAccountAlias
)