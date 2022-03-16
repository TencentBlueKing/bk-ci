package com.tencent.devops.common.ci.v2.utils

import com.tencent.devops.common.ci.v2.exception.YamlFormatException

object GitCIEnvUtils {
    fun checkEnv(env: Map<String, Any?>?, fileName: String? = null): Boolean {
        if (env != null) {
            if (env.size > 20) {
                throw YamlFormatException("${fileName ?: ""}配置Env数量超过20限制!")
            }

            env.forEach { (t, u) ->
                if (t.length > 128) {
                    throw YamlFormatException("${fileName ?: ""}Env单变量key长度超过128字符!($t)")
                }

                if (u != null && u.toString().length > 4000) {
                    throw YamlFormatException("${fileName ?: ""}Env单变量value长度超过4K字符!($t)")
                }
            }
            return true
        } else {
            return true
        }
    }
}
