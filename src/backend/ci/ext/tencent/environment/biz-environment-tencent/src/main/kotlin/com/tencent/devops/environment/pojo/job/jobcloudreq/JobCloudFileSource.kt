package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
class JobCloudFileSource(
    @get:Schema(title = "文件列表", required = true)
    @JsonProperty("file_list")
    val fileList: List<String>,
    @get:Schema(title = "源文件服务器", required = true)
    @JsonProperty("server")
    val server: JobCloudExecuteTarget,
    @get:Schema(title = "文件源账号", required = true)
    @JsonProperty("account")
    val account: JobCloudAccountAlias
)