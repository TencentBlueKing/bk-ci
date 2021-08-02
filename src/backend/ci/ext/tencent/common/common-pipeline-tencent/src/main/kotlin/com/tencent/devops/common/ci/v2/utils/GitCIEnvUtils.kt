package com.tencent.devops.common.ci.v2.utils

import com.tencent.devops.common.api.exception.CustomException
import javax.ws.rs.core.Response

object GitCIEnvUtils {
    fun checkEnv(env: Map<String, Any?>?): Boolean {
        if (env != null) {
            if (env.size > 20) {
                throw CustomException(Response.Status.BAD_REQUEST, "配置Env数量超过20限制!")
            }

            env.forEach { (t, u) ->
                if (t.length > 128) {
                    throw CustomException(Response.Status.BAD_REQUEST, "Env单变量key长度超过128字符!($t)")
                }

                if (u != null && u.toString().length > 4000) {
                    throw CustomException(Response.Status.BAD_REQUEST, "Env单变量value长度超过4K字符!($t)")
                }
            }

            return true
        } else {
            return true
        }
    }
}
