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

package com.tencent.devops.remotedev.utils

import org.slf4j.LoggerFactory

/**
 * Token加密工具类
 * 实现奇偶位对换加密算法
 */
object TokenEncryptUtil {

    private val logger = LoggerFactory.getLogger(TokenEncryptUtil::class.java)

    /**
     * 加密结果数据类
     */
    data class EncryptResult(
        val originalToken: String,
        val encryptedToken: String,
        val encryptTime: Long = System.currentTimeMillis()
    )

    /**
     * 对Token进行奇偶位对换加密
     * 
     * @param originalToken 原始token字符串
     * @return 加密后的token字符串
     */
    fun encryptToken(originalToken: String): String {
        if (!validateTokenFormat(originalToken)) {
            logger.warn("Invalid token format for encryption")
            return originalToken
        }

        try {
            val chars = originalToken.toCharArray()
            val length = chars.size

            // 奇偶位对换：奇数位和偶数位字符对换
            for (i in 0 until length - 1 step 2) {
                val temp = chars[i]
                chars[i] = chars[i + 1]
                chars[i + 1] = temp
            }

            val encryptedToken = String(chars)
            logger.debug("Token encrypted successfully, original length: $length")
            return encryptedToken
        } catch (e: Exception) {
            logger.error("Failed to encrypt token", e)
            return originalToken
        }
    }

    /**
     * 对加密Token进行解密（用于测试验证）
     * 
     * @param encryptedToken 加密后的token字符串
     * @return 解密后的原始token字符串
     */
    fun decryptToken(encryptedToken: String): String {
        if (!validateTokenFormat(encryptedToken)) {
            logger.warn("Invalid token format for decryption")
            return encryptedToken
        }

        try {
            val chars = encryptedToken.toCharArray()
            val length = chars.size

            // 奇偶位对换解密：奇数位和偶数位字符对换（与加密过程相同）
            for (i in 0 until length - 1 step 2) {
                val temp = chars[i]
                chars[i] = chars[i + 1]
                chars[i + 1] = temp
            }

            val decryptedToken = String(chars)
            logger.debug("Token decrypted successfully, original length: $length")
            return decryptedToken
        } catch (e: Exception) {
            logger.error("Failed to decrypt token", e)
            return encryptedToken
        }
    }

    /**
     * 验证Token格式是否有效
     * 
     * @param token 待验证的token字符串
     * @return token格式是否有效
     */
    fun validateTokenFormat(token: String): Boolean {
        return token.isNotEmpty()
    }
}
