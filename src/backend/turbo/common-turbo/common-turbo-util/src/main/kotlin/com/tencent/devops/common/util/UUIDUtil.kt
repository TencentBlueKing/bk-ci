package com.tencent.devops.common.util

import java.util.UUID

/**
 *
 * Powered By Tencent
 */
object UUIDUtil {
    /**
     * 生成32位字符随机UUID
     * @return UUID字符串
     */
    fun generate(): String {
        val uuid = UUID.randomUUID()
        val str = uuid.toString()
        // 去掉"-"符号
        return str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(
            19,
            23
        ) + str.substring(24)
    }
}
