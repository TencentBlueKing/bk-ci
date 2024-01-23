package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudAccount(
    @ApiModelProperty(value = "账号ID")
    val id: Long,
    @ApiModelProperty(value = "账号名称")
    val name: String?,
    @ApiModelProperty(value = "账号别名")
    val alias: String?
)