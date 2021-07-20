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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.SecretKeySpec

object DHUtil {
    private val KEY_ALGORITHM = "DH"
    private val KEY_PROVIDER = "BC"
    private val SECRECT_ALGORITHM = "DES"
    // private val KEY_SIZE = 1024
    private val p = BigInteger("16560215747140417249215968347342080587", 16)
    private val g = BigInteger("1234567890", 16)

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun initKey(): DHKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEY_PROVIDER)
        val serverParam = DHParameterSpec(p, g, 128)
        keyPairGenerator.initialize(serverParam, SecureRandom())
        // keyPairGenerator.initialize(KEY_SIZE)
        val keyPair = keyPairGenerator.generateKeyPair()
        return DHKeyPair(keyPair.public.encoded, keyPair.private.encoded)
    }

    fun initKey(partyAPublicKey: ByteArray): DHKeyPair {
        val x509KeySpec = X509EncodedKeySpec(partyAPublicKey)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        val publicKey = keyFactory.generatePublic(x509KeySpec)

        val dhParameterSpec = (publicKey as DHPublicKey).params
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEY_PROVIDER)
        keyPairGenerator.initialize(dhParameterSpec)
        // keyPairGenerator.initialize(KEY_SIZE)
        val keyPair = keyPairGenerator.genKeyPair()
        return DHKeyPair(keyPair.public.encoded, keyPair.private.encoded)
    }

    fun encrypt(data: ByteArray, partAPublicKey: ByteArray, partBPrivateKey: ByteArray): ByteArray {
        val key = getSecretKey(partAPublicKey, partBPrivateKey)
        val secretKey = SecretKeySpec(key, SECRECT_ALGORITHM)
        val cipher = Cipher.getInstance(secretKey.algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    fun decrypt(data: ByteArray, partBPublicKey: ByteArray, partAPrivateKey: ByteArray): ByteArray {
        val key = getSecretKey(partBPublicKey, partAPrivateKey)
        val secretKey = SecretKeySpec(key, SECRECT_ALGORITHM)
        val cipher = Cipher.getInstance(secretKey.algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    private fun getSecretKey(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        // 实例化密钥工厂
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        // 初始化公钥
        val x509KeySpec = X509EncodedKeySpec(publicKey)
        // 产生公钥
        val pubKey = keyFactory.generatePublic(x509KeySpec)
        // 初始化私钥
        val pkcs8KeySpec = PKCS8EncodedKeySpec(privateKey)
        // 产生私钥
        val priKey = keyFactory.generatePrivate(pkcs8KeySpec)
        // 实例化
        val keyAgree = KeyAgreement.getInstance(KEY_ALGORITHM, KEY_PROVIDER)
        // 初始化
        keyAgree.init(priKey)
        keyAgree.doPhase(pubKey, true)
        // 生成本地密钥
        val secretKey = keyAgree.generateSecret(SECRECT_ALGORITHM)
        return secretKey.encoded
    }
}