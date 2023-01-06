package com.tencent.devops.common.api.util

object LocaleUtil {

    private const val USER_LOCALE_KEY_PREFIX = "USER_LOCALE"

    /**
     * 获取用户国际化信息存在缓存中的key
     * @param userId 用户ID
     * @return 用户国际化信息存在缓存中的key
     */
    fun getUserLocaleKey(userId: String): String {
        return "$USER_LOCALE_KEY_PREFIX:$userId"
    }
}
