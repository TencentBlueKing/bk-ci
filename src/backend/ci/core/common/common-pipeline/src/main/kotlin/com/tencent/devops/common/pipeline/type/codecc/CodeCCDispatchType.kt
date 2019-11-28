package com.tencent.devops.common.pipeline.type.codecc

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.DispatchType

data class CodeCCDispatchType(
    val codeccTaskId: Long
) : DispatchType("", DispatchRouteKeySuffix.CODECC) {
    override fun buildType(): BuildType {
        return BuildType.DOCKER
    }

    override fun replaceField(variables: Map<String, String>) {
        val valueMap = mutableMapOf<String, Any?>()
        valueMap["codeccTaskId"] = codeccTaskId
        value = JsonUtil.toJson(valueMap)
    }
}