package com.tencent.devops.remotedev.utils

object CommonUtil {

    // 将带地区的ip转为不带地区的 NJ1.21.13.86.124 -> 21.13.86.124
    fun zoneIp2Ip(envIp: String): String {
        return envIp.split(".").let { it.subList(1, it.size).joinToString(separator = ".") }
    }

    fun zoneIp2Ip(envIp: String?): String? {
        return envIp?.split(".").let { it?.subList(1, it.size)?.joinToString(separator = ".") }
    }
}
