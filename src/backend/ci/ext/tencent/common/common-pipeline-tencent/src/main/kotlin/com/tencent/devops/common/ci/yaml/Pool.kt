package com.tencent.devops.common.ci.yaml

data class Pool(
    val container: String?,
    val credential: Credential?
)