package com.tencent.devops.common.pipeline.type.tstack

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchType

data class TStackDispatchType(@JsonProperty("value") val tstackAgentId: String) : DispatchType(tstackAgentId) {
    override fun replaceField(variables: Map<String, String>) {
    }

    override fun buildType() = BuildType.TSTACK
}