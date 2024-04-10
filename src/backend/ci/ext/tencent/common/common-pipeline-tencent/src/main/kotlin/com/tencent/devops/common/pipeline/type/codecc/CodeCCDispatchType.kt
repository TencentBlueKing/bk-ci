package com.tencent.devops.common.pipeline.type.codecc

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.DispatchType

data class CodeCCDispatchType(
    val codeccTaskId: Long,
    val extraInfo: Map<String, Any>? = emptyMap()
) : DispatchType("", DispatchRouteKeySuffix.CODECC) {
    override fun cleanDataBeforeSave() = Unit

    override fun buildType(): BuildType {
        return BuildType.valueOf(BuildType.DOCKER.name)
    }

    override fun replaceField(variables: Map<String, String>) {
        val valueMap = mutableMapOf<String, Any?>()
        valueMap["codeccTaskId"] = codeccTaskId
        if (extraInfo != null && extraInfo.isNotEmpty()) {
            valueMap.putAll(extraInfo)
        }
        value = JsonUtil.toJson(valueMap)
    }
}
