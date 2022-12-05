package com.tencent.devops.auth.utils

import com.tencent.devops.auth.pojo.TokenInfo
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.net.URLEncoder

internal class AccessTokenUtilsTest {

    @Test
    fun encryptAndDecrypt() {
        val secret = "qIU&(*^(*yo"
        val userDetails = "testUserId"
        val expirationTime = 100000L
        val res = URLEncoder.encode(AESUtil.encrypt(
            secret,
            JsonUtil.toJson(TokenInfo(
                userId = userDetails,
                expirationTime = expirationTime,
                accessToken = null
            ))
        ), "UTF-8")
        val result = AESUtil.decrypt(secret, URLDecoder.decode(res, "UTF-8"))
        val tokenInfo = JsonUtil.to(result, TokenInfo::class.java)
        print(tokenInfo)
        Assertions.assertEquals(tokenInfo.expirationTime, expirationTime)
        Assertions.assertEquals(tokenInfo.userId, userDetails)
    }
}
