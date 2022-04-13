package com.devops.process.yaml.v2.stageCheck

import com.devops.process.yaml.v2.models.GateNotices
import com.devops.process.yaml.v2.models.gate.ContinueOnFail
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gate(
    val name: String,
    val rule: List<String>,
    @JsonProperty("notify-on-fail")
    val notifyOnFail: List<GateNotices>,
    @JsonProperty("continue-on-fail")
    val continueOnFail: ContinueOnFail?
)
