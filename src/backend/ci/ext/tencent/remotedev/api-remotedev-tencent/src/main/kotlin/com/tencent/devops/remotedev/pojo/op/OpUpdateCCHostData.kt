package com.tencent.devops.remotedev.pojo.op

data class OpUpdateCCHostData(
    val action: OpOpUpdateCCHostDataAction,
    val scope: OpOpUpdateCCHostDataScope,
    val projectId: String?,
    val host: Set<String>?
)

enum class OpOpUpdateCCHostDataAction {
    DELETE, UPDATE
}

enum class OpOpUpdateCCHostDataScope {
    ALL, PART
}
