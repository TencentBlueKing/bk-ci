package com.tencent.devops.dispatch.devcloud.pojo

data class EnvironmentOpPatch(
    val op: String,
    val path: String,
    val value: String?
)

enum class PatchOp(val value: String) {
    ADD("add"),
    REPLACE("replace"),
    REMOVE("remove")
}
