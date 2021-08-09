package com.tencent.devops.common.ci.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.ci.v2.GateNotices
import com.tencent.devops.common.ci.v2.Notices

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gate(
    val name: String,
    val rule: List<String>,
    @JsonProperty("notify-on-fail")
    val notifyOnFail: List<GateNotices>?
)
