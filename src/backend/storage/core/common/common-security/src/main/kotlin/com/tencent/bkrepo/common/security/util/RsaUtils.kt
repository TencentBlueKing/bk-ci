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

import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.asymmetric.RSA
import com.tencent.bkrepo.common.security.crypto.CryptoProperties

/**
 * RSA 非对称加密工具类
 */
class RsaUtils(
    cryptoProperties: CryptoProperties
) {
    init {
        publicKey = cryptoProperties.publicKeyStr
        privateKey = cryptoProperties.privateKeyStr
        rsa = RSA(
            cryptoProperties.rsaAlgorithm,
            cryptoProperties.privateKeyStr,
            cryptoProperties.publicKeyStr
        )
    }

    companion object {
        lateinit var rsa: RSA
        lateinit var publicKey: String
        lateinit var privateKey: String
        /**
         * 公钥加密
         * @param password 需要解密的密码
         */
        fun encrypt(password: String): String {
            return rsa.encryptBcd(password, KeyType.PublicKey)
        }

        /**
         * 私钥解密，返回解密后的密码
         * @param password 前端加密后的密码
         */
        fun decrypt(password: String): String {
            return rsa.decryptStr(password, KeyType.PrivateKey)
        }
    }
}
