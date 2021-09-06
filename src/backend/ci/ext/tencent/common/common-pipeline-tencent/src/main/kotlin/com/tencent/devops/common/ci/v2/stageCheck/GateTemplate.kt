package com.tencent.devops.common.ci.v2.stageCheck

import com.tencent.devops.common.ci.v2.Parameters

data class GateTemplate(
    val gates: List<Gate>,
    val parameters: List<Parameters>?
)
