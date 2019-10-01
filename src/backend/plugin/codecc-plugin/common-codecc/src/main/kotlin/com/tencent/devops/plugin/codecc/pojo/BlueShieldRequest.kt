package com.tencent.devops.plugin.codecc.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BlueShieldRequest(
    @JsonProperty("proj_id_list")
    val taskIds: Collection<String>,
    @JsonProperty("start_time")
    val startTime: String,
    @JsonProperty("end_time")
    val endTime: String
)