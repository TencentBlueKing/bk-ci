package com.tencent.devops.remotedev.pojo

data class UserAuthInfo(
    val groupIds: Set<Int>
)

enum class UserAuthRecordStatus(val value: Int) {
    RUNNING(0),
    SUCCESS(1),
    FAILURE(2),
    TIME_OUT(3)
}