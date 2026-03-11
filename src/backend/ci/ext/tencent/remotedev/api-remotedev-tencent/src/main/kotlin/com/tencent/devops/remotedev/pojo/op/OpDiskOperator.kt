package com.tencent.devops.remotedev.pojo.op

data class OpDiskOperatorData(
    val ip: String,
    val op: OpDiskOperator,
    val disk: OpDiskOperatorDiskType,
    val size: Int
)

enum class OpDiskOperator {
    CREATE,
    UPDATE;
}

enum class OpDiskOperatorDiskType {
    D,
    E;
}

data class OpDiskOperatorDataResp(
    val result: Boolean,
    val message: String?,
    val taskId: String?
)