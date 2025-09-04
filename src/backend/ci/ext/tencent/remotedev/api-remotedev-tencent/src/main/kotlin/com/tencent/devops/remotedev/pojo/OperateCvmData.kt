package com.tencent.devops.remotedev.pojo

data class OperateCvmData(
    val projectId: String,
    val opType: OperateCvmDataType,
    val ipList: Set<String>
)

enum class OperateCvmDataType {
    ADD,
    DELETE
}