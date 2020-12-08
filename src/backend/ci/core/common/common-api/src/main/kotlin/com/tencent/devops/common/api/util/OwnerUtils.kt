package com.tencent.devops.common.api.util

object OwnerUtils {
    val projectOwnerKey = "PROJECT_V3_OWNER:"

    fun getOwnerRedisKey(projectId: String): String {
        return projectOwnerKey + projectId
    }
}