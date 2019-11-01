package com.tencent.devops.process.engine.pojo

object Timeout {
    const val MAX_MINUTES = 7 * 60 * 24 // 2 * 24 * 60 = 2880 分钟 = 最多超时2天
    const val MAX_MILLS = MAX_MINUTES * 60 * 1000 + 1 // 毫秒+1
    const val DEFAULT_TIMEOUT_MIN = 900
}