package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudFileSource(
    @ApiModelProperty(value = "文件类型", notes = "1-服务器文件，2-本地文件，3-文件源文件")
    @JsonProperty("file_type")
    val fileType: Int,
    @ApiModelProperty(value = "文件路径列表")
    @JsonProperty("file_list")
    val fileList: List<String>,
    @ApiModelProperty(value = "源文件所在机器")
    val server: JobCloudVariableServer?,
    @ApiModelProperty(value = "执行账号ID")
    val account: JobCloudAccount,
    @ApiModelProperty(value = "第三方文件源ID")
    @JsonProperty("file_source_id")
    val fileSourceId: Long?,
    @ApiModelProperty(value = "第三方文件源code")
    @JsonProperty("file_source_code")
    val fileSourceCode: String?
)