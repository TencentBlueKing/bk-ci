package com.tencent.devops.dispatch.codecc.pojo

import java.time.LocalDateTime

data class CodeCCIpInfoModel(
    val id: Int,
    val codeccDispatchIp: String,
    val containerNum: Int,
    val averageMem: Int,
    val averageCpu: Int,
    val enabled: Int,
    val createdDate: LocalDateTime,
    val createdBy: String,
    val updatedDate: LocalDateTime,
    val updatedBy: String
)
