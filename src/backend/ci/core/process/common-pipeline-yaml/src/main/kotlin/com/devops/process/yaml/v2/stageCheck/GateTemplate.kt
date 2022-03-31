package com.devops.process.yaml.v2.stageCheck

import com.devops.process.yaml.v2.parameter.Parameters

data class GateTemplate(
    val gates: List<Gate>,
    val parameters: List<Parameters>?
)
