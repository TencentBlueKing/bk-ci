package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "工作空间共享信息")
data class CgsResourceConfig(
    @JsonProperty("zone")
    val zoneList: List<String>,
    @JsonProperty("machineType")
    val machineTypeList: List<String>
)
