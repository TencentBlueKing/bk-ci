package com.tencent.devops.auth.utils

import java.nio.charset.Charset
import java.util.Base64

object StringUtils {
    fun decodeAuth(token: String): Pair<String, String> {
        val str = if(token.contains("Basic ")) {
            token.substringAfter("Basic ")
        } else {
            token
        }
        val decodeStr = String(Base64.getDecoder().decode(str), Charset.forName("UTF-8"))
        return Pair(decodeStr.substringBefore(":"), decodeStr.substringAfter(":"))
    }
}