package com.tencent.devops.auth.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.pojo.TokenInfo
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder

internal class AccessTokenUtilsTest {
    private val objectMapper = ObjectMapper()
    val secret = "testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56"

    @Test
    fun isValidToken() {
        val key = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0VXNlciIsImlhdCI6MTYzMDQwNTE0MSwiZXhwIjoxNjMwNDA1MjQxfQ.2iRYDNgdw3MaLTN9UtGVgYjX96G6wVDL_WkTLDelZF24tdBMqzg35lAuIKsU_K35F_UN1ZiIgF5X1JmYVry-GQ"
        print(AccessTokenUtils.isValidToken(key, secret))
    }

    @Test
    fun generateToken() {
        val userId = "testUser"
        val accessToken = AccessTokenUtils.generateToken(userId, 100000, secret)
        println(accessToken)
        println(AccessTokenUtils.isValidToken(accessToken, secret))
    }

    @Test
    fun encrypt() {
        val secret = "qIU&(*^(*yo"
        val userDetails = "testUserId"
        val expirationTime = 100000
        val res = URLEncoder.encode(AESUtil.encrypt(
            secret,
            objectMapper.writeValueAsString(TokenInfo(
                userId = userDetails,
                expirationTime = System.currentTimeMillis() + (expirationTime ?: 14400000)
            ))
        ), "UTF-8")
        print(res)
    }

    @Test
    fun decrypt() {
        val token = "GWwCkJhMddoxMbuUiEieF4XpjFnvUv%2BSwYdtN3Sy%2F54mEN3SoXrFNFvH6S%2FDN7eHex9ozS2ssZkIKQNSU6BjQg%3D%3D"
        val secret = "qIU&(*^(*yo"
        val result = AESUtil.decrypt(secret, URLDecoder.decode(token, "UTF-8"))
        val expireTime = JsonUtil.to(result, TokenInfo::class.java)
        print(expireTime)
    }
}