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

package com.tencent.bkrepo.common.security.util

import cn.hutool.crypto.CryptoException
import cn.hutool.crypto.asymmetric.RSA
import com.tencent.bkrepo.common.security.crypto.CryptoProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RsaUtils工具类测试")
internal class RsaUtilsTest {

    private var rsaAlgorithm: String = "RSA/ECB/PKCS1Padding"
    private var privateKeyStr: String? = null
    private var publicKeyStr: String? = null
    private var cryptoProperties: CryptoProperties? = null

    @Test
    @DisplayName("测试默认算法加解密-无key")
    fun testRsaUtilsWithNullKey() {
        cryptoProperties = CryptoProperties(
            rsaAlgorithm = rsaAlgorithm,
            privateKeyStr = privateKeyStr,
            publicKeyStr = publicKeyStr
        )
        RsaUtils(cryptoProperties!!)
        val publicKey = RsaUtils.publicKey
        val privateKey = RsaUtils.privateKey
        val encryptResult = RsaUtils.encrypt("test")
        val decryptResult = RsaUtils.decrypt(encryptResult)
        Assertions.assertEquals("test", decryptResult)
        RsaUtils(cryptoProperties!!)
        val publicKey1 = RsaUtils.publicKey
        val privateKey1 = RsaUtils.privateKey
        val encryptResult1 = RsaUtils.encrypt("test")
        val decryptResult1 = RsaUtils.decrypt(encryptResult1)
        Assertions.assertNotEquals(publicKey, publicKey1)
        Assertions.assertNotEquals(privateKey, privateKey1)
        Assertions.assertNotEquals(encryptResult, encryptResult1)
        Assertions.assertEquals(decryptResult, decryptResult1)
        Assertions.assertThrows(CryptoException::class.java) { RsaUtils.decrypt(encryptResult) }
    }

    @Test
    @DisplayName("测试默认算法加解密-有key")
    fun testRsaUtilsWithKey() {
        val rsa = RSA()
        privateKeyStr = rsa.privateKeyBase64
        publicKeyStr = rsa.publicKeyBase64
        println("$privateKeyStr")
        println("$publicKeyStr")
        cryptoProperties = CryptoProperties(
            rsaAlgorithm = rsaAlgorithm,
            privateKeyStr = privateKeyStr,
            publicKeyStr = publicKeyStr
        )
        RsaUtils(cryptoProperties!!)
        val encryptResult = RsaUtils.encrypt("test")
        val decryptResult = RsaUtils.decrypt(encryptResult)
        val publicKey = RsaUtils.publicKey
        val privateKey = RsaUtils.privateKey
        Assertions.assertEquals("test", decryptResult)
        RsaUtils(cryptoProperties!!)
        val publicKey1 = RsaUtils.publicKey
        val privateKey1 = RsaUtils.privateKey
        val encryptResult1 = RsaUtils.encrypt("test")
        val decryptResult1 = RsaUtils.decrypt(encryptResult1)
        val decryptResult2 = RsaUtils.decrypt(encryptResult)
        Assertions.assertEquals(publicKey, publicKey1)
        Assertions.assertEquals(privateKey, privateKey1)
        Assertions.assertNotEquals(encryptResult, encryptResult1)
        Assertions.assertEquals(decryptResult, decryptResult1)
        Assertions.assertEquals(decryptResult, decryptResult2)
    }
}
