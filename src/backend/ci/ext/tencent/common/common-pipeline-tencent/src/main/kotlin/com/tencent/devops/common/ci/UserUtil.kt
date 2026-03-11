package com.tencent.devops.common.ci

object UserUtil {
    fun isTaiUser(userId: String) = userId.endsWith("@tai")

    fun removeTaiSuffix(userId: String) = userId.removeSuffix("@tai")
}
