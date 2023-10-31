package com.tencent.devops.environment.pojo.job.jobCloudReq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
class JobCloudAccount(
    @ApiModelProperty(value = "执行帐号ID")
    @JsonProperty("id")
    val id: Long?,
    @ApiModelProperty(value = "执行帐号别名")
    @JsonProperty("alias")
    val alias: String?
)