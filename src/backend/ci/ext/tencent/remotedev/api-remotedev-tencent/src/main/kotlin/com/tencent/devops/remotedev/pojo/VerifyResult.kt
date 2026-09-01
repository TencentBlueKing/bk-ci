package com.tencent.devops.remotedev.pojo

data class VerifyResult(
    val result: Boolean,
    val type: VerifyResultType?
)

enum class VerifyResultType {
    INVALID,
    EXPIRED,
    ;
}
