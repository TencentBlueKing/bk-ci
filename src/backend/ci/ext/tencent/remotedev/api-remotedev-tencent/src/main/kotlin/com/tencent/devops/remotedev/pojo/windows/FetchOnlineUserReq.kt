package com.tencent.devops.remotedev.pojo.windows

data class FetchOnlineUserReq(
    val timeScope: TimeScope
)


enum class TimeScope {
    HOUR,
    DAY,
    WEEK
}