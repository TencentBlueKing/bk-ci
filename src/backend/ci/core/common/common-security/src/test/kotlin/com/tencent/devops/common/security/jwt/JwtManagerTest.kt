/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.common.security.jwt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JwtManagerTest {

    private val publicKeyString: String/*RSA公钥2048bit(PKCS#8)*/ =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA86kLZelXctsgna8vBaRc\n" +
            "8NerNXIoiiFqH9gge14LuHLI+NjSAbEacDj4fcJ6P8CSfzcMH8tpKvlosfTN9bMh\n" +
            "GraDbkKa7zPr2DPChZobXoeF+3ClGb2HZTufblzJZ6cgEPfXjeKPFKUdO9VDQZTj\n" +
            "WAqLapEydwdHJDVDCqtsIWzSbQfVzr9//DtKYeazT39kCnr6nnTz344eIVbvdADH\n" +
            "xhq8CWrPkETByKgYnYXWdfn1+ly7TSDOUe5MFMnfyjXcrwd3ADptabeskHghc8P7\n" +
            "lx3Ibq7c3MnE/98bYCXl+ZCo7IY0rfVElto94UvrxxgYPeAi3X+lk8RnmjgHS/5+\n" +
            "IQIDAQAB"
    private val privateKeyString: String/*RSA私钥2048bit(PKCS#8)*/ =
        "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDzqQtl6Vdy2yCd\n" +
            "ry8FpFzw16s1ciiKIWof2CB7Xgu4csj42NIBsRpwOPh9wno/wJJ/Nwwfy2kq+Wix\n" +
            "9M31syEatoNuQprvM+vYM8KFmhteh4X7cKUZvYdlO59uXMlnpyAQ99eN4o8UpR07\n" +
            "1UNBlONYCotqkTJ3B0ckNUMKq2whbNJtB9XOv3/8O0ph5rNPf2QKevqedPPfjh4h\n" +
            "Vu90AMfGGrwJas+QRMHIqBidhdZ1+fX6XLtNIM5R7kwUyd/KNdyvB3cAOm1pt6yQ\n" +
            "eCFzw/uXHchurtzcycT/3xtgJeX5kKjshjSt9USW2j3hS+vHGBg94CLdf6WTxGea\n" +
            "OAdL/n4hAgMBAAECggEBALHYkgw5q+2WiZDunBi3JmtYT4v8HiDsUMSbgOHq/A0O\n" +
            "IyiXQXmgphaBTsakG4zK52LZcA0I8GNAli1F7MrIi4Iu83GYRfQQZrVw2iugFxgB\n" +
            "PUcQqkFGeDvor+7i5NK2Ro58CCZ01lCQT+0rNSL6JJJPaAFJp5b/heqkwaFZC9y8\n" +
            "X5jPnUJk1Z0VJdrnsqcWf9grCK+irUyhnk0Nq9KAbOBbDBaT9jzcjtxRvSZugmLz\n" +
            "uJSEcyhjrRM1ciNqixCO3X6iCf2oEApdBKueE0qgVyxgxxqmsWKTVgKGT2qMOZ6y\n" +
            "FdTiBRIFHKSsl3CU4nHGbNkIS/id5FCDrIipeWsZGAECgYEA/ihTa+CH5LDm7sc3\n" +
            "M/GthIdKeabIDI/tvkpyA7LyXGBXgxXvLWmxKCU5oFi7s0OROlEL8lUni0m8YLcl\n" +
            "E7zlvYKwkcTrTKM1GWwHpwY+6GZLFXJ3HDJWE2K1ir7+k3xHGEV7A+H5eF6m3ZSx\n" +
            "/t9YohzE9WK5MZZP9vKsnMJ7/2ECgYEA9W081Dn/SPP2vCjtG+fd65JaAHrE+dpn\n" +
            "EYNljOeMzHEMv1yfbT/x4hBjuJRcZreblRRZmd/RHbRNyLQQr3UJBLYqP3rCSGcY\n" +
            "ABAM9psIbG1NntbJ83JrujApdq4CqBiZ+5Bu9pHc4fF/axUAGxkN2Ess4vJOjEYn\n" +
            "HjWS+O10tsECgYEAqwH3H160EOv2djMUsZ6rYcCmG7RcZhdxn9f3XwXIjN9GAq1/\n" +
            "gM7cpGZnn7wUj0mnLdXac/NX6CB0355bFCzFZ/3HUE1vBOHLmI9XlspdCYHKg2PB\n" +
            "QPedcu23uONJ53J3Y51caABkGtmU7QJfwV4GBQ3WeEU01miM5VvjSJaTWAECgYEA\n" +
            "y4pc+GVhIs+xwTrv6kTR95zYp60pz00iTZP7lHA0hRj8yXe3gJOXtzSAf4QLXeTI\n" +
            "U0sdRFAqzcfK+rjbXahiYlXxk9PrbMCTVvn4xkytH79GsITR1+T9Etz+hj6qVV6R\n" +
            "1eBjJnqyBXCTi+tOuyp8IPW3tD/ghNVpe9RSn7/PHAECgYEA2cc/77XUyPKkakrp\n" +
            "yiRiB7ulRVP/6naljTLSPJdOy+dD3GUQfCS9MiDxTWPR7dhenjZ0VSpgYqB1nSuS\n" +
            "wPml0ASz44JZEExii5cNN21wullKmAPmlfeAm7kjTGmu9pQ3NyqruP29OKL3MiB/\n" +
            "wrWQEX7q1vF+8sB1/KR7+M4YS7E="
    private val enable: Boolean = true

    @Test
    fun generateAndVerify() {
        // 生成并验证jwt token
        val jwtManager = JwtManager(privateKeyString, publicKeyString, enable)
        val jwtToken = jwtManager.getToken()
        val verifyResult = jwtManager.verifyJwt(jwtToken!!)
        Assertions.assertEquals(true, verifyResult)
    }

    @Test
    fun isAuthEnable() {
        // 判断是否需要验证
        val jwtManager = JwtManager(privateKeyString, publicKeyString, enable)
        val isAuthEnable = jwtManager.isAuthEnable()
        Assertions.assertEquals(true, isAuthEnable)
    }

    @Test
    fun isSendEnable() {
        // 判断是否需要发送头部验证
        val jwtManager = JwtManager(privateKeyString, publicKeyString, enable)
        val isSendEnable = jwtManager.isSendEnable()
        Assertions.assertEquals(true, isSendEnable)
    }

    @Test
    fun refreshToken() {
        // 生成并验证jwt token
        val jwtManager = JwtManager(privateKeyString, publicKeyString, enable)
        val jwtToken = jwtManager.getToken()
        val verifyResult = jwtManager.verifyJwt(jwtToken!!)
        Assertions.assertEquals(true, verifyResult)
        jwtManager.refreshToken()
        // 刷新并验证新jwt token
        val refreshJwtToken = jwtManager.getToken()
        val refreshVerifyResult = jwtManager.verifyJwt(refreshJwtToken!!)
        Assertions.assertEquals(true, refreshVerifyResult)
    }
}
