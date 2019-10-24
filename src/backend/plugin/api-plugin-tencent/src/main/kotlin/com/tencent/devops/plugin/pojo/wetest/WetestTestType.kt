package com.tencent.devops.plugin.pojo.wetest

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest云设备信息")
data class WetestTestType(
    @ApiModelProperty("testtype")
    @JsonProperty("testtype")
    val testType: String,
    @ApiModelProperty("testname")
    @JsonProperty("testname")
    val testName: String,
    @ApiModelProperty("frametype")
    @JsonProperty("frametype")
    val frametype: List<String>?
)
