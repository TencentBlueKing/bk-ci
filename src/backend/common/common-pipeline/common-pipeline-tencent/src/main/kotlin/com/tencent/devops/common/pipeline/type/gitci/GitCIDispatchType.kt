package com.tencent.devops.common.pipeline.type.gitci

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.DispatchType

data class GitCIDispatchType(@JsonProperty("value") var image: String)
    : DispatchType(
    image,
    DispatchRouteKeySuffix.GITCI
) {
    override fun replaceField(variables: Map<String, String>) {
        image = EnvUtils.parseEnv(image, variables)
    }

    override fun buildType() = BuildType.GIT_CI
}