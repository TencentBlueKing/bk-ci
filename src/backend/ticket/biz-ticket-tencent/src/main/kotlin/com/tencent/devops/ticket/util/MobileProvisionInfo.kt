package com.tencent.devops.ticket.util

import java.time.LocalDateTime

/**
 * Created by Aaron Sheng on 2017/11/26.
 */
data class MobileProvisionInfo(
    val expireDate: LocalDateTime,
    val name: String,
    val uuid: String,
    val teamName: String
)
