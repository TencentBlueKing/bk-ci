package com.tencent.devops.environment.pojo.envOperate

import java.time.LocalDateTime

data class EnvOperateLog(
    val id: Long,
    val projectId: String,
    val envId: Long,
    val operateOrigin: EnvOperateOrigin,
    val operateName: EnvOperateName,
    val operateContent: EnvOperateContent?,
    val operator: String,
    val createTime: LocalDateTime
)
