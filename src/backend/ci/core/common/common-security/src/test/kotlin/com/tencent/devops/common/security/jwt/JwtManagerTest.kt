/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5plU4eyJ+wS9xF3Xa7OOsrdpo7TnAfRcLMYg8ySPt00vHkuGL8GenvR0Kb" +
            "mL8F+YKRmKNl/uoDAjq71jWy7SNXF5+5Wi3/A79AUaX0LslL9tx5TZcy/Z1sqi9v+f0R7B186iCo+ZL0ZXqupcH+QqS0CM4UZ5" +
            "NvABDPBnUHJgVc9Fbfzjo6czR48NRYykpbKp9zbXnvCunSbsV+BxxcorRCynigXBUfL/FZxEWXDiwe8744HNpH16qGMUIMqBuW" +
            "NPgEVzNE/5O/H6YvhwhAIVHXIBxFaAACJr92V21WzvGr6vE9u7lpTo6Zdykt0Axw6A/P6+OeWEoQ0foEg3fL3jZwIDAQAB"
    private val privateKeyString: String/*RSA私钥2048bit(PKCS#8)*/ =
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDmmVTh7In7BL3EXddrs46yt2mjtOcB9FwsxiDzJI+3TS8eS4YvwZ" +
            "6e9HQpuYvwX5gpGYo2X+6gMCOrvWNbLtI1cXn7laLf8Dv0BRpfQuyUv23HlNlzL9nWyqL2/5/RHsHXzqIKj5kvRleq6lwf5CpL" +
            "QIzhRnk28AEM8GdQcmBVz0Vt/OOjpzNHjw1FjKSlsqn3Ntee8K6dJuxX4HHFyitELKeKBcFR8v8VnERZcOLB7zvjgc2kfXqoYx" +
            "QgyoG5Y0+ARXM0T/k78fpi+HCEAhUdcgHEVoAAImv3ZXbVbO8avq8T27uWlOjpl3KS3QDHDoD8/r455YShDR+gSDd8veNnAgMB" +
            "AAECggEACWmKyGnyO9e2szQtXc0Hbw4DdtYkc7T4qi8wGxyL7SxuVus4P+zEP7K0pxtWteoA+PR86rlBsTIl0pW5X3hqxhmqo/" +
            "U4n7FbkRtAN1Ew/OVPYs0VjxkN6DF/M1u9tzhtKxTZEltIImDZpTVJq1eRzl6FIZDR08c6E8AgR21Tt0zZrk+k/vN5v0jszvcb" +
            "9svGrKY2FXaCjMY/1vagRcps9MeKLyvOa7w7/7/akqloIgq0RU/3XpuTmesFXXIZD2gwsHqtm1gS0WZ519snSjjVoE5VmdgK9+" +
            "d7aImpZQhP18hbQTfDSe+gcF+WyNtkm/i9TJUM27a0dFZY09R0wzh3YQKBgQD5S0pP112PTSbHx4ZRMjv/nx//Tj03Q4qsluUR" +
            "u8JqMwo0O63TXIJSt5Gqs4B3uIbGES5hfq5Fv1DMdzToJfXFwP/64N6ZHyj1aLvpH+poaYdyo0t8b95P5cyS506OYRTj6ezMnu" +
            "MgZlm/HjdSoWD5XA/Sg9n/7ZZqDbH/rceFlwKBgQDszU0a3ud68gRZypfk1G723ThdqGbne/x3JPHg0BB8t4LTIV1sbG3sZJAq" +
            "9/TviiQyEFFAdACRgGkmN9jsyZIWLAxUDZyd49Cm96cgMH9t0FfbxLnII2k2MXV8yzdFogGMO7tl22AEzeKUHmdVlsNS3dyBBv" +
            "Q/N9ZMed5qXpZqsQKBgANzOk0OkvnnvHCpXz+CbXglMKEs6QX5xlKLKBJtwcwMdsiwRsCVGCyi4740C8QcZqkZFcY9pZXLhwe9" +
            "YpsP423gNOq47/u4ha2XzHn2eh0F0N0SS3omZjI+4OzrjJfF2i8pTqCqVkRdhkJx3ZwkJZ5t2r40GvFCzYEcPWlnc0pzAoGAb7" +
            "uTNSmWGZnWWbPRJeTiktLhBLgwDeufvWwOQSCLlv1T2mE03rLuzwrIR64FwXzyBTlTzVCu7/iiJKlXTqrxpkqUapidnJZg3lee" +
            "YP7hMaSLwCO78WZb/3ko0YBljRKbHlsU2kO/s6Sd03vhEC557UDICY2Jc8bZ8+3Q92m4GZECgYEAm5wPgrz+PVBtuCAEHLREHG" +
            "ec8ggGrIy7W6K3A15+HWR7p7F9GfkxDaZ+X18lf1Bjrjucjz1aDpHA/TtjvvyXPBlgUAI7f54AqhvCztUMLcNxc7mzOuwGh70j" +
            "/UT8Vtnhar1zpDh8m++9lS1nSfR8SVSyXNRlNspMM6cWyV8CW40="
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
