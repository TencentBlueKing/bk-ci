package com.tencent.devops.common.pipeline.type.idc

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType

data class IDCDispatchType(
    @JsonProperty("value") var image: String,
    var imageType: ImageType? = ImageType.BKDEVOPS,
    val credentialId: String? = ""
) : DispatchType(image, DispatchRouteKeySuffix.IDC) {
    override fun replaceField(variables: Map<String, String>) {
        val valueMap = mutableMapOf<String, Any?>()
        valueMap["image"] = EnvUtils.parseEnv(image, variables)
        valueMap["imageType"] = imageType
        valueMap["credentialId"] = credentialId

        image = JsonUtil.toJson(valueMap)
    }

    override fun buildType() = BuildType.IDC
}