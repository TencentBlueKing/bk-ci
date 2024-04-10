package com.tencent.devops.common.ci

object UserUtil {
    fun isTaiUser(userId: String) = userId.endsWith("@tai")
}
