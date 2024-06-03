package com.tencent.devops.common.api.util

object LocaleUtil {

    private const val USER_LOCALE_LANGUAGE_KEY_PREFIX = "USER_LOCALE_LANGUAGE"

    /**
     * 获取用户国际化语言信息存在缓存中的key
     * @param userId 用户ID
     * @return 用户国际化语言信息存在缓存中的key
     */
    fun getUserLocaleLanguageKey(userId: String): String {
        return "$USER_LOCALE_LANGUAGE_KEY_PREFIX:$userId"
    }
}
