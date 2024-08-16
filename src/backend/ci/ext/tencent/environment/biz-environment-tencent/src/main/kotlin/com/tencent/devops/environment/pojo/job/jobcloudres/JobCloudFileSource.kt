package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudFileSource(
    @get:Schema(title = "文件类型", description = "1-服务器文件，2-本地文件，3-文件源文件")
    @JsonProperty("file_type")
    val fileType: Int,
    @get:Schema(title = "文件路径列表")
    @JsonProperty("file_list")
    val fileList: List<String>,
    @get:Schema(title = "源文件所在机器")
    val server: JobCloudVariableServer?,
    @get:Schema(title = "执行账号ID")
    val account: JobCloudAccount,
    @get:Schema(title = "第三方文件源ID")
    @JsonProperty("file_source_id")
    val fileSourceId: Long?,
    @get:Schema(title = "第三方文件源code")
    @JsonProperty("file_source_code")
    val fileSourceCode: String?
)