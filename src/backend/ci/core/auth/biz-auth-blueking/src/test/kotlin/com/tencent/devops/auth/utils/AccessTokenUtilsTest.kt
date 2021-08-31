package com.tencent.devops.auth.utils

import org.junit.Test

internal class AccessTokenUtilsTest {
    val secret = "testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56testSecretKey^&gs56"

    @Test
    fun isValidToken() {
        val key = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0VXNlciIsImlhdCI6MTYzMDQwNTE0MSwiZXhwIjoxNjMwNDA1MjQxfQ.2iRYDNgdw3MaLTN9UtGVgYjX96G6wVDL_WkTLDelZF24tdBMqzg35lAuIKsU_K35F_UN1ZiIgF5X1JmYVry-GQ"
        print(AccessTokenUtils.isValidToken(key,secret))
    }

    @Test
    fun generateToken() {
        val userId = "testUser"
        val accessToken = AccessTokenUtils.generateToken(userId,100000,secret)
        println(accessToken)
        println(AccessTokenUtils.isValidToken(accessToken,secret))
    }
}