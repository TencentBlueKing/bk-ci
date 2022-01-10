package com.tencent.devops.common.ci.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.ci.v2.ContinueOnFail
import com.tencent.devops.common.ci.v2.GateNotices

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gate(
    val name: String,
    val rule: List<String>,
    @JsonProperty("notify-on-fail")
    val notifyOnFail: List<GateNotices>,
    @JsonProperty("continue-on-fail")
    val continueOnFail: ContinueOnFail?
)
