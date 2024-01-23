package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CCBkHost(
    @ApiModelProperty(value = "CC中的机器host_id", required = true)
    @JsonProperty("bk_host_ids")
    val bkHostIds: List<Long>
)