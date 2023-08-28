package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

@ApiModel("工作空间共享信息")
data class CgsResourceConfig(
    @JsonProperty("zoneList")
    val zoneList: List<String>,
    @JsonProperty("machineTypeList")
    val machineTypeList: List<String>
)
