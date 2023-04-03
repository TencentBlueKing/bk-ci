package com.tencent.devops.dispatch.codecc.pojo

data class CodeccDispatchMessage (
    val codeccTaskId: Long,
    val image: String?,
    val userName: String?,
    val password: String?,
    val closedSourceParams: List<CodeCCRuntimeParam>?
)
