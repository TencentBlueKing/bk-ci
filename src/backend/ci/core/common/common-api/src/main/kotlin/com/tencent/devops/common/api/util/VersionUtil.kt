package com.tencent.devops.common.api.util

object VersionUtil {
    // 比较版本大小, 版本格式为 x.y.z
    // appVersion > compareVersion , 返回 1
    // appVersion = compareVersion , 返回 0
    // appVersion < compareVersion , 返回 -1
    // appVersion = null 或者 version 不符合规范, 返回 -2
    fun compare(
        appVersion: String?,
        compareVersion: String
    ): Int {
        if (null == appVersion) {
            return -2
        }

        val appVersionArray = appVersion.split(".")
        if (appVersionArray.size != 3) {
            return -2
        }

        val compareVersionArray = compareVersion.split(".")
        if (compareVersionArray.size != 3) {
            return -2
        }

        try {
            for (i in 0..2) {
                val appV = appVersionArray[i].toInt()
                val compareV = compareVersion[i].toInt()

                if (appV > compareV) {
                    return 1
                } else if (appV < compareV) {
                    return -1
                }
            }
        } catch (e: Exception) {
            return -2
        }

        return 0
    }
}