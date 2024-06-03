package com.tencent.devops.common.api.util

interface KeyReplacement {
    /**
     * 如果[key]替换不成功需要返回null，不建议直接返回[key]，避免无法判断到底替换成功
     */
    fun getReplacement(key: String): String?
}
